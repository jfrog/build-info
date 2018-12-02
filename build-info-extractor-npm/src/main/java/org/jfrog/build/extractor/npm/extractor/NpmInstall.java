package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.util.VersionException;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class NpmInstall extends NpmCommand {
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private Properties npmAuth;
    private String npmRegistry;

    /**
     * Install npm package.
     *
     * @param clientBuilder        - Build Info client builder.
     * @param resolutionRepository - The repository it'll resolve from.
     * @param executablePath       - Npm executable path.
     * @param args                 - Npm args.
     * @param logger               - The logger.
     * @param path                 - Path to directory contains package.json or path to '.tgz' file.
     */
    public NpmInstall(ArtifactoryDependenciesClientBuilder clientBuilder, String resolutionRepository, String args, String executablePath, Log logger, Path path) {
        super(clientBuilder, executablePath, args, resolutionRepository, logger, path);
    }

    public Module execute() {
        try (ArtifactoryDependenciesClient dependenciesClient = (ArtifactoryDependenciesClient) clientBuilder.build()) {
            client = dependenciesClient;
            preparePrerequisites();
            createTempNpmrc();
            runInstall();
            restoreNpmrc();

            ModuleBuilder builder = new ModuleBuilder();
            builder.id(npmPackageInfo.getModuleId());
            builder.dependencies(getBuildDependencies());
            return builder.build();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    private void preparePrerequisites() throws InterruptedException, VersionException, IOException {
        validateArtifactoryVersion();
        validateNpmVersion();
        validateRepoExists("Source repo must be specified");
        setNpmAuth();
        setRegistryUrl();
        readPackageInfoFromPackageJson();
        backupProjectNpmrc();
    }

    private void setNpmAuth() throws IOException {
        npmAuth = ((ArtifactoryDependenciesClient) client).getNpmAuth();
    }

    private void setRegistryUrl() {
        npmRegistry = client.getArtifactoryUrl();
        if (!StringUtils.endsWith(npmRegistry, "/")) {
            npmRegistry += "/";
        }
        npmRegistry += "api/npm/" + repo;
    }

    private void readPackageInfoFromPackageJson() throws IOException {
        try (FileInputStream fis = new FileInputStream(workingDir.resolve("package.json").toFile())) {
            npmPackageInfo.readPackageInfo(fis);
        }
    }

    /**
     * To make npm do the resolution from Artifactory we are creating .npmrc file in the project dir.
     * If a .npmrc file already exists we will backup it and override while running the command.
     */
    private void backupProjectNpmrc() throws IOException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        if (!Files.exists(npmrcPath)) {
            return;
        }
        Path npmrcBackupPath = workingDir.resolve(NPMRC_BACKUP_FILE_NAME);
        Files.copy(npmrcPath, npmrcBackupPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createTempNpmrc() throws IOException, InterruptedException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        Files.deleteIfExists(npmrcPath); // Delete old npmrc file
        final String configList = npmDriver.configList(workingDir.toFile(), args);

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
     * npm install scope can be set by "--production" or "--only={prod[uction]|dev[elopment]}" flags.
     *
     * @param npmrcProperties - The results of 'npm config list' command.
     */
    private void setScope(Properties npmrcProperties) {
        String only = npmrcProperties.getProperty("only");
        if (StringUtils.startsWith(only, "prod") || Boolean.parseBoolean(npmrcProperties.getProperty("production"))) {
            npmPackageInfo.setScope(NpmScope.PRODUCTION.toString());
        } else if (StringUtils.startsWith(only, "dev")) {
            npmPackageInfo.setScope(NpmScope.DEVELOPMENT.toString());
        }
    }

    private void runInstall() throws IOException {
        npmDriver.install(workingDir.toFile(), args);
    }

    private void restoreNpmrc() throws IOException {
        Path npmrcPath = workingDir.resolve(NPMRC_FILE_NAME);
        Path npmrcBackupPath = workingDir.resolve(NPMRC_BACKUP_FILE_NAME);
        if (Files.exists(npmrcBackupPath)) { // npmrc file did exist before - Restore it.
            Files.move(npmrcBackupPath, npmrcPath, StandardCopyOption.REPLACE_EXISTING);
        } else { // npmrc file didn't exist before - Delete the temporary npmrc file.
            Files.deleteIfExists(npmrcPath);
        }
    }

    private List<Dependency> getBuildDependencies() throws IOException {
        List<NpmScope> scopes = new ArrayList<>();
        if (StringUtils.isBlank(npmPackageInfo.getScope())) {
            scopes.add(NpmScope.DEVELOPMENT);
            scopes.add(NpmScope.PRODUCTION);
        } else {
            scopes.add(NpmScope.valueOf(npmPackageInfo.getScope().toUpperCase()));
        }
        NpmProject npmProject = new NpmProject();
        for (NpmScope scope : scopes) {
            List<String> extraListArgs = new ArrayList<>();
            extraListArgs.add("--only=" + scope);
            JsonNode jsonNode = npmDriver.list(workingDir.toFile(), extraListArgs);
            npmProject.addDependencies(Pair.of(scope, jsonNode));
        }

        return new NpmBuildInfoExtractor(clientBuilder, logger).extract(npmProject);
    }
}
