package org.jfrog.build.extractor.go.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.packageManager.PackageManagerUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.Character.isUpperCase;
import static java.lang.Character.toLowerCase;
import static org.jfrog.build.api.util.FileChecksumCalculator.*;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;


@SuppressWarnings({"unused", "WeakerAccess"})
public class GoRun extends GoCommand {

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
    private String goCmdArgs;
    private String resolutionRepository;
    private String resolverUsername;
    private String resolverPassword;
    private Map<String, String> env;

    /**
     * Run go command and collect dependencies.
     *
     * @param goCmdArgs                 - Go cmd args.
     * @param path                      - Path to directory contains go.mod.
     * @param artifactoryManagerBuilder - Manager builder for resolution.
     * @param repo                      - Artifactory's repository for resolution.
     * @param username                  - Artifactory's username for resolution.
     * @param password                  - Artifactory's password for resolution.
     * @param logger                    - The logger.
     * @param env                       - Environment variables to use during npm execution.
     */
    public GoRun(String goCmdArgs, Path path, String buildInfoModuleId, ArtifactoryManagerBuilder artifactoryManagerBuilder, String repo, String username, String password, Log logger, Map<String, String> env) {
        super(artifactoryManagerBuilder, path, buildInfoModuleId, logger);
        this.env = env;
        this.goCmdArgs = goCmdArgs;
        this.resolutionRepository = repo;
        this.resolverUsername = username;
        this.resolverPassword = password;
    }

    /**
     * Allow running go run using a new Java process.
     * Used only in Jenkins to allow running 'rtGo run' in a docker container.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.resolver);
            ArtifactoryClientConfiguration.PackageManagerHandler packageManagerHandler = clientConfiguration.packageManagerHandler;
            ArtifactoryClientConfiguration.GoHandler goHandler = clientConfiguration.goHandler;
            GoRun goRun = new GoRun(
                    packageManagerHandler.getArgs(),
                    Paths.get(packageManagerHandler.getPath() != null ? packageManagerHandler.getPath() : "."),
                    packageManagerHandler.getModule(),
                    artifactoryManagerBuilder,
                    clientConfiguration.resolver.getRepoKey(),
                    clientConfiguration.resolver.getUsername(),
                    clientConfiguration.resolver.getPassword(),
                    clientConfiguration.getLog(),
                    clientConfiguration.getAllProperties()
            );
            goRun.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    public BuildInfo execute() {
        try (ArtifactoryManager artifactoryClient = (artifactoryManagerBuilder != null ? artifactoryManagerBuilder.build() : null)) {
            if (artifactoryClient != null) {
                preparePrerequisites(resolutionRepository, artifactoryClient);
                setResolverAsGoProxy(artifactoryClient);
            }
            // We create the GoDriver here as env might had changed.
            this.goDriver = new GoDriver(GO_CLIENT_CMD, env, path.toFile(), logger);
            // First try to run 'go version' to make sure go is in PATH, and write the output to logger.
            goDriver.version(true);
            goDriver.runCmd(goCmdArgs, true);
            this.moduleName = goDriver.getModuleName();
            collectDependencies();
            return createBuild();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * In order to use Artifactory as a resolver we need to set GOPROXY env var with Artifactory details.
     * Wa also support fallback to VCS in case pkg doesn't exist in Artifactort,
     */
    private void setResolverAsGoProxy(ArtifactoryManager artifactoryClient) throws Exception {
        String rtUrl = PackageManagerUtils.createArtifactoryUrlWithCredentials(artifactoryClient.getUrl(), resolverUsername, resolverPassword, ARTIFACTORY_GO_API + resolutionRepository);
        String proxyValue = rtUrl + "," + GOPROXY_VCS_FALLBACK;
        env.put(GOPROXY_ENV_VAR, proxyValue);
    }

    /**
     * Run 'go mod graph' and parse its output.
     * This command might change go.mod and go.sum content, so we backup and restore those files.
     * The output format is:
     * * For direct dependencies:
     * <module-name> <dependency's-module-name>@v<dependency-module-version>
     * * For transient dependencies:
     * <dependency's-module-name>@v<dependency-module-version> <dependency's-module-name>@v<dependency-module-version>
     * In order to populate build info dependencies, we parse the second column uf the mod graph output.
     */
    private void collectDependencies() throws Exception {
        backupModAnsSumFiles();
        CommandResults goGraphResult = goDriver.modGraph(true);
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
        CommandResults goEnvResult = goDriver.runCmd(GO_GET_GOPATH_CMD, true);
        return goEnvResult.getRes().trim() + File.separator + CACHE_INNER_PATH + File.separator;
    }

    /**
     * According to Go convention, module name in cache path contains only lower case letters,
     * each upper case letter is replaced with "! + lower case letter". (e.g: "AbC" => "!ab!c")
     */
    private String convertModuleNameToCachePathConvention(String moduleName) {
        String upperCaseSign = "!";
        for (int i = 0; i < moduleName.length(); i++) {
            if (isUpperCase(moduleName.charAt(i))) {
                moduleName = moduleName.replace(moduleName.substring(i, i + 1), upperCaseSign + toLowerCase(moduleName.charAt(i)));
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
        File moduleZip = new File(cachedPkgPath);
        if (moduleZip.exists()) {
            Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(moduleZip, MD5_ALGORITHM, SHA1_ALGORITHM, SHA256_ALGORITHM);
            Dependency dependency = new DependencyBuilder()
                    .id(moduleName + ':' + moduleVersion)
                    .md5(checksums.get(MD5_ALGORITHM)).sha1(checksums.get(SHA1_ALGORITHM)).sha256(checksums.get(SHA256_ALGORITHM))
                    .type("zip")
                    .build();
            dependenciesList.add(dependency);
        }
    }

    private BuildInfo createBuild() {
        BuildInfo buildInfo = new BuildInfo();
        String moduleId = StringUtils.defaultIfBlank(buildInfoModuleId, moduleName);
        Module module = new ModuleBuilder()
                .type(ModuleType.GO)
                .id(moduleId)
                .dependencies(dependenciesList)
                .build();
        buildInfo.setModules(Collections.singletonList(module));
        return buildInfo;
    }
}
