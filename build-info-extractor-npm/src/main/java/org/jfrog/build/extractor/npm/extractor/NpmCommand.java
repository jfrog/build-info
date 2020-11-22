package org.jfrog.build.extractor.npm.extractor;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Base class for npm commands.
 *
 * @author Yahav Itzhak
 */
abstract class NpmCommand extends PackageManagerExtractor {
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

    NpmCommand(ArtifactoryClientBuilderBase clientBuilder, String repo, Log logger, Path path, Map<String, String> env) {
        this.clientBuilder = clientBuilder;
        this.npmDriver = new NpmDriver(env);
        this.workingDir = Files.isDirectory(path) ? path : path.toAbsolutePath().getParent();
        this.repo = repo;
        this.logger = logger;
        this.path = path;
    }

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
}
