package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.Log;
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

@SuppressWarnings("unused")
public class NpmInstall implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private transient ArtifactoryDependenciesClient dependenciesClient;
    private String resolutionRepository;
    private List<String> installArgs;
    private NpmDriver npmDriver;
    private Log logger;
    private File ws;

    private PackageInfo npmPackageInfo = new PackageInfo();
    private Properties npmAuth;
    private String npmRegistry;

    public NpmInstall(ArtifactoryDependenciesClient dependenciesClient, String resolutionRepository, String installArgs, String executablePath, Log logger, File ws) {
        this.resolutionRepository = resolutionRepository;
        this.dependenciesClient = dependenciesClient;
        this.installArgs = Lists.newArrayList(installArgs.split("\\s+"));
        this.npmDriver = new NpmDriver(executablePath);
        this.logger = logger;
        this.ws = ws;
    }

    public Module execute() throws InterruptedException, VersionException, IOException {
        preparePrerequisites();
        createTempNpmrc();
        runInstall();
        restoreNpmrc();

        ModuleBuilder builder = new ModuleBuilder();
        builder.id(npmPackageInfo.toString());
        builder.dependencies(getBuildDependencies());
        return builder.build();
    }

    private void preparePrerequisites() throws InterruptedException, VersionException, IOException {
        validateNpmVersion();
        setNpmAuth();
        setRegistryUrl();
        readPackageInfoFromPackageJson();
        backupProjectNpmrc();

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

    private void readPackageInfoFromPackageJson() throws IOException {
        npmPackageInfo.readPackageInfo(ws);
    }

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
        Files.deleteIfExists(npmrcPath); // Delete old npmrc file
        final String configList = npmDriver.configList(ws, installArgs);

        Properties npmrcProperties = new Properties();

        // Save npm config list results
        ObjectMapper mapper = new ObjectMapper();
        JsonNode manifestTree = mapper.readTree(configList);
        manifestTree.fields().forEachRemaining(entry -> npmrcProperties.setProperty(entry.getKey(), entry.getValue().asText()));

        // Save npm auth
        npmrcProperties.putAll(npmAuth);

        // Save registry
        npmrcProperties.setProperty("registry", npmRegistry);
        npmrcProperties.remove("metrics-registry");

        // Write npmrc file
        StringBuilder stringBuffer = new StringBuilder();
        npmrcProperties.forEach((key, value) -> stringBuffer.append(key).append("=").append(value).append("\n"));
        setScope(npmrcProperties);
        try (FileWriter fileWriter = new FileWriter(npmrcPath.toFile());
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
            npmPackageInfo.setScope(NpmScope.PRODUCTION.toString());
        } else if (StringUtils.equals(only, "dev")) {
            npmPackageInfo.setScope(NpmScope.DEVELOPMENT.toString());
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

    private List<Dependency> getBuildDependencies() throws IOException {
        List<NpmScope> scopes = new ArrayList<>();
        if (StringUtils.isBlank(npmPackageInfo.getScope())) {
            scopes.add(NpmScope.DEVELOPMENT);
            scopes.add(NpmScope.PRODUCTION);
        } else {
            scopes.add(NpmScope.valueOf(npmPackageInfo.getScope()));
        }
        NpmProject npmProject = new NpmProject(dependenciesClient, logger);
        for (NpmScope scope : scopes) {
            List<String> extraListArgs = new ArrayList<>();
            extraListArgs.add("--only=" + scope);
            JsonNode jsonNode = npmDriver.list(ws, extraListArgs);
            npmProject.addDependencies(Pair.of(scope, jsonNode));
        }

        NpmBuildInfoExtractor buildInfoExtractor = new NpmBuildInfoExtractor();
        return buildInfoExtractor.extract(npmProject);
    }
}
