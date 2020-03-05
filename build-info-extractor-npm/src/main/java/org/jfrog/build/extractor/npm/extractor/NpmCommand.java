package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/**
 * Base class for npm install and npm publish commands.
 *
 * @author Yahav Itzhak
 */
abstract class NpmCommand implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_ARTIFACTORY_VERSION = new ArtifactoryVersion("5.5.2");
    private static final ArtifactoryVersion MIN_SUPPORTED_NPM_VERSION = new ArtifactoryVersion("5.4.0");

    NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
    ArtifactoryClientBuilderBase clientBuilder;
    ArtifactoryBaseClient client;
    NpmDriver npmDriver;
    Path workingDir;
    String repo;
    Log logger;
    Path path;

    NpmCommand(ArtifactoryClientBuilderBase clientBuilder, String executablePath, String repo, Log logger, Path path, Map<String, String> env) {
        this.clientBuilder = clientBuilder;
        this.npmDriver = new NpmDriver(executablePath, env);
        this.workingDir = Files.isDirectory(path) ? path : path.toAbsolutePath().getParent();
        this.repo = repo;
        this.logger = logger;
        this.path = path;
    }

    abstract Build execute();

    void validatePath() throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("Path " + path + " doesn't exist");
        }
    }

    void validateArtifactoryVersion() throws VersionException {
        ArtifactoryVersion version = client.getArtifactoryVersion();
        if (version.isNotFound()) {
            String message = "Couldn't execute npm task. Check connection with Artifactory.";
            throw new VersionException(message, VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(MIN_SUPPORTED_ARTIFACTORY_VERSION)) {
            String message = String.format("Couldn't execute npm task. Artifactory version is %s but must be at least %s.", version.toString(), MIN_SUPPORTED_ARTIFACTORY_VERSION.toString());
            throw new VersionException(message, VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    void validateNpmVersion() throws IOException, InterruptedException, VersionException {
        String npmVersionStr = npmDriver.version(workingDir.toFile());
        ArtifactoryVersion npmVersion = new ArtifactoryVersion(npmVersionStr);
        if (!npmVersion.isAtLeast(MIN_SUPPORTED_NPM_VERSION)) {
            throw new VersionException("Couldn't execute npm task. Npm version must be at least " + MIN_SUPPORTED_NPM_VERSION.toString() + ".", VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    void validateRepoExists(String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }

    /**
     * Create a new client configuration from the 'buildInfoConfig.propertiesFile' and environment variables.
     *
     * @return a new client configuration
     */
    static ArtifactoryClientConfiguration createArtifactoryClientConfiguration() {
        NpmBuildInfoLogger log = new NpmBuildInfoLogger();
        ArtifactoryClientConfiguration clientConfiguration = new ArtifactoryClientConfiguration(log);
        Properties allNpmProps = new Properties();
        allNpmProps.putAll(System.getenv());
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allNpmProps, log);
        clientConfiguration.fillFromProperties(allProps);
        return clientConfiguration;
    }

    /**
     * Run npm install or publish command and save build info to file.
     *
     * @param clientConfiguration - The client configuration.
     * @throws RuntimeException in case of any error.
     */
    void executeAndSaveBuildInfo(ArtifactoryClientConfiguration clientConfiguration) throws RuntimeException {
        Build build = execute();
        if (build == null) {
            throw new RuntimeException();
        }
        saveBuildInfoToFile(clientConfiguration, build);
    }

    /**
     * Save the build info calculated in the npm install or npm publish.
     *
     * @param clientConfiguration - The client configuration
     * @param build               - The build to save
     */
    static void saveBuildInfoToFile(ArtifactoryClientConfiguration clientConfiguration, Build build) {
        String generatedBuildInfoPath = clientConfiguration.info.getGeneratedBuildInfoFilePath();
        if (StringUtils.isBlank(generatedBuildInfoPath)) {
            return;
        }
        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(generatedBuildInfoPath));
        } catch (Exception e) {
            clientConfiguration.getLog().error("Failed writing build info to file: ", e);
            throw new RuntimeException(e);
        }
    }
}
