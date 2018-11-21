package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.npm.utils.NpmDriver;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NpmInstall implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String NPMRC_FILE_NAME = ".npmrc";
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");

    private transient ArtifactoryDependenciesClient dependenciesClient;
    private transient NpmDriver npmDriver = new NpmDriver();
    private transient ObjectMapper mapper = new ObjectMapper();

    private List<String> installArgs;
    private String resolutionRepository;
    private File ws;
    private PrintStream logger;

    private String executablePath;
    private String npmRegistry;
    private Properties npmAuth;
    private boolean collectBuildInfo;
    private List<Dependency> dependencies;
    private NpmScope scope;
    private PackageInfo npmPackageInfo;

    public NpmInstall(List<String> installArgs, ArtifactoryDependenciesClient dependenciesClient, String resolutionRepository, File ws, PrintStream logger) {
        this.installArgs = installArgs;
        this.dependenciesClient = dependenciesClient;
        this.resolutionRepository = resolutionRepository;
        this.ws = ws;
        this.logger = logger;
    }

    public void execute() throws InterruptedException, VersionException, IOException {
        preparePrerequisites();
        createTempNpmrc();
        runInstall();
        restoreNpmrc();
        setDependencies();
    }

    private void preparePrerequisites() throws InterruptedException, VersionException, IOException {
        setNpmExecutable();
        validateNpmVersion();
        setNpmAuth();
        setRegistryUrl();
//        readPackageInfoFromPackageJson(); // TODO
        backupProjectNpmrc();

    }

    private void setNpmExecutable() {
        executablePath = "npm";
    }

    private void validateNpmVersion() throws IOException, InterruptedException, VersionException {
        String npmVersionStr = npmDriver.version(ws);
        ArtifactoryVersion npmVersion = new ArtifactoryVersion(npmVersionStr);
        if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
            throw new VersionException("Couldn't execute npm task. Version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".", VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    private void setNpmAuth() throws IOException {
        npmAuth = dependenciesClient.getNpmAuth();
    }

    private void setRegistryUrl() {
        if (StringUtils.isNoneBlank(resolutionRepository)) {
            npmRegistry = dependenciesClient.getArtifactoryUrl();
            if (!StringUtils.endsWith(npmRegistry, "/")) {
                npmRegistry += "/";
            }
            npmRegistry += "api/npm" + resolutionRepository;
        }
    }

//    private void readPackageInfoFromPackageJson() throws IOException, InterruptedException {
//        NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
//        npmPackageInfo.readPackageInfo(listener, ws);
//        npmInstall.setNpmPackageInfo(npmPackageInfo);
//    }

    /**
     * To make npm do the resolution from Artifactory we are creating .npmrc file in the project dir.
     * If a .npmrc file already exists we will backup it and override while running the command.
     */
    private void backupProjectNpmrc() throws IOException {
        Path npmrcPath = ws.toPath().resolve(NPMRC_FILE_NAME);
        if (!Files.exists(npmrcPath)) {
            return;
        }
        Path npmrcBackupPath = ws.toPath().resolve(NPMRC_BACKUP_FILE_NAME);
        Files.copy(npmrcPath, npmrcBackupPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createTempNpmrc() throws IOException, InterruptedException {
        Path npmrcPath = ws.toPath().resolve(NPMRC_FILE_NAME);
        Files.delete(npmrcPath); // Delete old npmrc file
        final String configList = npmDriver.configList(ws, installArgs);

        Properties npmrcProperties = new Properties();

        // Save npm config list results
        JsonNode manifestTree = mapper.readTree(configList);
        manifestTree.fields().forEachRemaining(entry -> npmrcProperties.setProperty(entry.getKey(), entry.getValue().asText()));

        // Save npm auth
        npmrcProperties.putAll(npmAuth);

        // Save registry
        npmrcProperties.setProperty("registry", npmRegistry);
        npmrcProperties.remove("metrics-registry");

        // Write npmrc file
        File npmrcFile = npmrcPath.resolve(NPMRC_FILE_NAME).toFile();
        StringBuilder stringBuffer = new StringBuilder();
        npmrcProperties.forEach((key, value) -> stringBuffer.append(key).append("=").append(value).append("\n"));
        setScope(npmrcProperties);
        try (FileWriter fileWriter = new FileWriter(npmrcFile);
             BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
            bufferedWriter.write(stringBuffer.toString());
            bufferedWriter.flush();
        }
    }

    /**
     * npm install type restriction can be set by "--production" or "-only={prod[uction]|dev[elopment]}" flags.
     * @param npmrcProperties - The results of 'npm config list' command.
     */
    private void setScope(Properties npmrcProperties) {
        String only = npmrcProperties.getProperty("only");
        if (StringUtils.equals(only, "prod") || Boolean.parseBoolean(npmrcProperties.getProperty("production"))) {
            scope = NpmScope.PRODUCTION;
        } else if (StringUtils.equals(only, "dev")) {
            scope = NpmScope.DEVELOPMENT;
        }
    }

    private void runInstall() throws IOException {
        npmDriver.install(ws, installArgs);
    }

    private void restoreNpmrc() throws IOException {
        Path npmrcPath = ws.toPath().resolve(NPMRC_FILE_NAME);
        Path npmrcBackupPath = ws.toPath().resolve(NPMRC_BACKUP_FILE_NAME);
        if (!Files.exists(npmrcBackupPath)) { // npmrc file didn't exist before
            Files.deleteIfExists(npmrcPath);
            return;
        }
        Files.move(npmrcBackupPath, npmrcPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void setDependencies() throws IOException {
        List<NpmScope> scopes = new ArrayList<>();
        if (scope == null) {
            scopes.add(NpmScope.DEVELOPMENT);
            scopes.add(NpmScope.PRODUCTION);
        } else {
            scopes.add(scope);
        }
        NpmProject npmProject = new NpmProject(dependenciesClient, logger);
        for (NpmScope scope : scopes) {
            List<String> listArgs = new ArrayList<>();
            listArgs.add("--only=" + scope);
            JsonNode jsonNode = npmDriver.list(ws, listArgs);
            npmProject.addDependencies(Pair.of(scope, jsonNode));
        }

        NpmBuildInfoExtractor buildInfoExtractor = new NpmBuildInfoExtractor();
        dependencies = buildInfoExtractor.extract(npmProject);
    }
}
