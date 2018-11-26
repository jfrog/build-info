package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.util.VersionException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmInstall extends NpmCommand {
    private static final String NPMRC_BACKUP_FILE_NAME = "jfrog.npmrc.backup";
    private static final String NPMRC_FILE_NAME = ".npmrc";

    private transient ArtifactoryDependenciesClient client;
    private Log logger;

    private Properties npmAuth;
    private String npmRegistry;

    public NpmInstall(ArtifactoryDependenciesClient client, String resolutionRepository, String installArgs, String executablePath, Log logger, File ws) {
        super(executablePath, installArgs, resolutionRepository, ws);
        this.client = client;
        this.logger = logger;
    }

    public Module execute() throws InterruptedException, VersionException, IOException {
        preparePrerequisites();
        createTempNpmrc();
        runInstall();
        restoreNpmrc();

        ModuleBuilder builder = new ModuleBuilder();
        builder.id(npmPackageInfo.getModuleId());
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

    private void setNpmAuth() throws IOException {
        npmAuth = client.getNpmAuth();
    }

    private void setRegistryUrl() {
        if (StringUtils.isNoneBlank(repo)) {
            npmRegistry = client.getArtifactoryUrl();
            if (!StringUtils.endsWith(npmRegistry, "/")) {
                npmRegistry += "/";
            }
            npmRegistry += "api/npm" + repo;
        }
    }

    private void readPackageInfoFromPackageJson() throws IOException {
        try (FileInputStream fis = new FileInputStream(Paths.get(ws.getPath(), "package.json").toFile())) {
            npmPackageInfo.readPackageInfo(fis);
        }
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
        final String configList = npmDriver.configList(ws, args);

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
        npmDriver.install(ws, args);
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
        NpmProject npmProject = new NpmProject(client, logger);
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
