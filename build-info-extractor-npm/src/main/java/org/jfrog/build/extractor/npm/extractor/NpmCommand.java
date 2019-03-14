package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

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
        this.workingDir = Files.isDirectory(path) ? path : path.getParent();
        this.repo = repo;
        this.logger = logger;
        this.path = path;
    }

    void validateArtifactoryVersion() throws VersionException {
        ArtifactoryVersion version = client.getArtifactoryVersion();
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
}
