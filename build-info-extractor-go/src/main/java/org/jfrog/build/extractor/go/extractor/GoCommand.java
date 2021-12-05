package org.jfrog.build.extractor.go.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Base class for go build and go publish commands.
 */
abstract class GoCommand extends PackageManagerExtractor {

    protected static final String SHA1 = "SHA1";
    protected static final String MD5 = "MD5";
    protected static final String LOCAL_GO_MOD_FILENAME = "go.mod";
    protected static final String GO_CLIENT_CMD = "go";
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_ARTIFACTORY_VERSION = new ArtifactoryVersion("6.10.0");

    ArtifactoryManagerBuilder artifactoryManagerBuilder;
    GoDriver goDriver;
    Path path;
    // Module name, as specified in go.mod, is used for naming the relevant go package files.
    String moduleName;
    // Module id is used to determine which buildInfo's module should be used for the current go operation.
    // By default it's value is moduleNme, unless customize differently.
    String buildInfoModuleId;
    Log logger;

    GoCommand(ArtifactoryManagerBuilder artifactoryManagerBuilder, Path path, String buildInfoModuleId, Log logger) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.logger = logger;
        this.path = path;
        this.buildInfoModuleId = buildInfoModuleId;
    }

    protected void preparePrerequisites(String repo, ArtifactoryManager artifactoryManager) throws VersionException, IOException {
        validateArtifactoryVersion(artifactoryManager);
        validateRepoExists(repo, artifactoryManager, "The provided repo must be specified");
    }

    private void validateArtifactoryVersion(ArtifactoryManager artifactoryManager) throws VersionException, IOException {
        ArtifactoryVersion version = artifactoryManager.getVersion();
        if (version.isNotFound()) {
            String message = "Couldn't execute go task. Check connection with Artifactory.";
            throw new VersionException(message, VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(MIN_SUPPORTED_ARTIFACTORY_VERSION)) {
            String message = String.format("Couldn't execute Go task. Artifactory version is %s but must be at least %s.", version.toString(), MIN_SUPPORTED_ARTIFACTORY_VERSION.toString());
            throw new VersionException(message, VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    private void validateRepoExists(String repo, ArtifactoryManager artifactoryManager, String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!artifactoryManager.isRepositoryExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }

    protected String getModFilePath() {
        return path.toString() + File.separator + LOCAL_GO_MOD_FILENAME;
    }
}
