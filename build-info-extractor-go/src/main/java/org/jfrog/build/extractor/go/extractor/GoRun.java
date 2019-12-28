package org.jfrog.build.extractor.go.extractor;

import org.apache.http.client.utils.URIBuilder;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.lang.Character.*;


@SuppressWarnings({"unused", "WeakerAccess"})
public class GoRun extends GoCommand {

    private static final String GO_CLIENT_CMD = "go";
    private static final String GO_MOD_GRAPH_CMD = "mod graph";
    private static final String GO_ENV_CMD = "env";
    private static final String GOPATH_ENV_VAR = "GOPATH";
    private static final String GOPROXY_ENV_VAR = "GOPROXY";
    private static final String GO_GET_GOPATH_CMD = GO_ENV_CMD + " " + GOPATH_ENV_VAR;
    private static final String GOPROXY_VCS_FALLBACK = "direct";
    private static final String CACHE_INNER_PATH = Paths.get("pkg", "mod", "cache", "download").toString();
    private static final String ARTIFACTORY_GO_API = "/api/go/";
    private static final String LOCAL_GO_SUM_FILENAME = "go.sum";
    private static final String LOCAL_GO_SUM_BACKUP_FILENAME = "jfrog.go.sum.backup";
    private static final String LOCAL_GO_MOD_BACKUP_FILENAME = "jfrog.go.mod.backup";

    private List<Dependency> dependenciesList = new ArrayList<>();
    private CommandExecutor goCommandExecutor;
    private String goCmdArgs;
    private String resolutionRepository;
    private String resolverUsername;
    private String resolverPassword;
    private Map<String, String> env;

    /**
     * Run go command and collect dependencies.
     *
     * @param goCmdArgs     - Go cmd args.
     * @param path          - Path to directory contains go.mod.
     * @param clientBuilder - Client builder for resolution.
     * @param repo          - Artifactory's repository for resolution.
     * @param username      - Artifactory's username for resolution.
     * @param password      - Artifactory's password for resolution.
     * @param logger        - The logger.
     * @param env           - Environment variables to use during npm execution.
     */
    public GoRun(String goCmdArgs, Path path, ArtifactoryBuildInfoClientBuilder clientBuilder, String repo, String username, String password, Log logger, Map<String, String> env) throws IOException {
        super(clientBuilder, path, logger);
        this.goCmdArgs = goCmdArgs;
        this.resolutionRepository = repo;
        this.resolverUsername = username;
        this.resolverPassword = password;
        this.env = env;
    }

    public Build execute() {
        try (ArtifactoryBuildInfoClient artifactoryClient = (clientBuilder != null ? (ArtifactoryBuildInfoClient) clientBuilder.build() : null)) {
            if (artifactoryClient != null) {
                preparePrerequisites(resolutionRepository, artifactoryClient);
                setResolverAsGoProxy(artifactoryClient);
            }
            this.goCommandExecutor = new CommandExecutor(GO_CLIENT_CMD, env);
            runGoCmd(goCmdArgs);
            collectDependencies();
            return createBuild(null, dependenciesList);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * In order to use Artifactory as a resolver we need to set GOPROXY env var with Artifactory details.
     * Wa also support fallback to VCS in case pkg doesn't exist in Artifactort,
     */
    private void setResolverAsGoProxy(ArtifactoryBuildInfoClient client) throws Exception {
        URL rtUrl = new URL(client.getArtifactoryUrl());
        URIBuilder proxyUrlBuilder = new URIBuilder()
                .setScheme(rtUrl.getProtocol())
                .setUserInfo(resolverUsername, resolverPassword)
                .setHost(rtUrl.getHost())
                .setPath(rtUrl.getPath() + ARTIFACTORY_GO_API + resolutionRepository);
        String proxyValue = proxyUrlBuilder.build().toURL().toString() + "," + GOPROXY_VCS_FALLBACK;
        env.put(GOPROXY_ENV_VAR, proxyValue);
    }

    /**
     *  Run 'go mod graph' and parse its output.
     *  This command might change go.mod and go.sum content, so we backup and restore those files.
     *    The output format is:
     *     * For direct dependencies:
     *          <module-name> <dependency's-module-name>@v<dependency-module-version>
     *     * For transient dependencies:
     *        <dependency's-module-name>@v<dependency-module-version> <dependency's-module-name>@v<dependency-module-version>
     *   In order to populate build info dependencies, we parse the second column uf the mod graph output.
     */
    private void collectDependencies() throws Exception {
        backupModAnsSumFiles();
        CommandResults goGraphResult = runGoCmd(GO_MOD_GRAPH_CMD);
        String cachePath = getCachePath();
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");
        for (String entry : dependenciesGraph) {
            String moduleToAdd = entry.split(" ")[1];
            addModuleDependencies(moduleToAdd, cachePath);
        }
        restoreModAnsSumFiles();
    }

    private void backupModAnsSumFiles() throws IOException {
        createBackupFile(path, LOCAL_GO_MOD_FILENAME, LOCAL_GO_MOD_BACKUP_FILENAME);
        createBackupFile(path, LOCAL_GO_SUM_FILENAME, LOCAL_GO_SUM_BACKUP_FILENAME);
    }

    private void restoreModAnsSumFiles() throws IOException {
        restoreFile(path, LOCAL_GO_MOD_FILENAME, LOCAL_GO_MOD_BACKUP_FILENAME);
        restoreFile(path, LOCAL_GO_SUM_FILENAME, LOCAL_GO_SUM_BACKUP_FILENAME);
    }

    /**
     * Create copy of parentPath/sourceFilename under parentPath/backupFilename
     */
    private void createBackupFile(Path parentPath, String sourceFilename, String backupFilename) throws IOException {
        File source = new File(path.toString() + File.separator + sourceFilename);
        File backup = new File(path.toString() + File.separator + backupFilename);
        Files.copy(source.toPath(), backup.toPath(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Restore the original sourceFilename content by renaming parentPath/backupFilename -> parentPath/sourceFilename
     */
    private void restoreFile(Path parentPath, String sourceFilename, String backupFilename) throws IOException {
        File source = new File(path.toString() + File.separator + sourceFilename);
        File backup = new File(path.toString() + File.separator + backupFilename);

        Files.move(backup.toPath(), source.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private String getCachePath() throws Exception {
        CommandResults goEnvResult = runGoCmd(GO_GET_GOPATH_CMD);
        return goEnvResult.getRes().trim() + File.separator + CACHE_INNER_PATH + File.separator;
    }

    /**
     *  According to Go convention, module name in cache path contains only lower case letters,
     *  each upper case letter is replaced with "! + lower case letter". (e.g: "AbC" => "!ab!c")
     */
    private String convertModuleNameToCachePathConvention(String moduleName) {
        String upperCaseSign = "!";
        for (int i = 0; i < moduleName.length(); i++) {
            if (isUpperCase(moduleName.charAt(i))) {
                moduleName = moduleName.replace( moduleName.substring(i, i + 1),  upperCaseSign + toLowerCase(moduleName.charAt(i)));
                i += upperCaseSign.length();
            }
        }
        return moduleName;
    }

    /**
     * Each module is in format <module-name>@v<module-version>.
     * We add only the pgk zip file as build's dependency.
     * The dependency's id is "module-name:version", and its type is "zip".
     * We locate each pkg zip file downloaded to local Go cache, and calculate the pkg checksum.
     */
    private void addModuleDependencies(String module, String cachePath) throws Exception {
        String moduleName = module.split("@")[0];
        String moduleVersion = module.split("@")[1];
        String cachedPkgPath = cachePath + convertModuleNameToCachePathConvention(moduleName) + File.separator + "@v" + File.separator + moduleVersion + ".zip";
        File moduleZip = new File (cachedPkgPath);
        if (moduleZip.exists()) {
            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(moduleZip, MD5, SHA1);
            Dependency dependency = new DependencyBuilder()
                    .id(moduleName + ':' + moduleVersion)
                    .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                    .type("zip")
                    .build();
            dependenciesList.add(dependency);
        }
    }

    /**
     * Run go client cmd with goArs.
     * goArgs must be a space-separated string.
     * Write stdout + stderr to logger, and return the command's result.
     */
    private CommandResults runGoCmd(String args) throws Exception {
        List goArgs = new ArrayList<>(Arrays.asList(args.split(" ")));
        CommandResults goCmdResult = goCommandExecutor.exeCommand(path.toFile(), goArgs, logger);
        if (!goCmdResult.isOk()) {
            throw new IOException(goCmdResult.getErr());
        }
        logger.info(goCmdResult.getErr() + goCmdResult.getRes());
        return goCmdResult;
    }
}
