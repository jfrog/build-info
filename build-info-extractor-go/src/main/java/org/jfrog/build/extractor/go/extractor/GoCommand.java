package org.jfrog.build.extractor.go.extractor;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Base class for go build and go publish commands.
 */
abstract class GoCommand implements Serializable {

    protected static final String SHA1 = "SHA1";
    protected static final String MD5 = "MD5";
    protected static final String LOCAL_GO_MOD_FILENAME = "go.mod";
    protected static final String GO_CLIENT_CMD = "go";
    private static final long serialVersionUID = 1L;
    private static final ArtifactoryVersion MIN_SUPPORTED_ARTIFACTORY_VERSION = new ArtifactoryVersion("6.10.0");

    ArtifactoryClientBuilderBase clientBuilder;
    GoDriver goDriver;
    Path path;
    String moduleName;
    Log logger;

    GoCommand(ArtifactoryBuildInfoClientBuilder clientBuilder, Path path, Log logger) throws IOException {
        this.clientBuilder = clientBuilder;
        this.logger = logger;
        this.path = path;
    }

    protected void preparePrerequisites(String repo, ArtifactoryBaseClient client) throws VersionException, IOException {
        validateArtifactoryVersion(client);
        validateRepoExists(repo, client, "The provided repo must be specified");
    }

    private void validateArtifactoryVersion(ArtifactoryBaseClient client) throws VersionException {
        ArtifactoryVersion version = client.getArtifactoryVersion();
        if (version.isNotFound()) {
            String message = "Couldn't execute go task. Check connection with Artifactory.";
            throw new VersionException(message, VersionCompatibilityType.NOT_FOUND);
        }
        if (!version.isAtLeast(MIN_SUPPORTED_ARTIFACTORY_VERSION)) {
            String message = String.format("Couldn't execute Go task. Artifactory version is %s but must be at least %s.", version.toString(), MIN_SUPPORTED_ARTIFACTORY_VERSION.toString());
            throw new VersionException(message, VersionCompatibilityType.INCOMPATIBLE);
        }
    }

    private void validateRepoExists(String repo, ArtifactoryBaseClient client, String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }

    protected Build createBuild(List<Artifact> artifacts, List<Dependency> dependencies) {
        ModuleBuilder moduleBuilder = new ModuleBuilder().id(moduleName);
        if (artifacts != null) {
            moduleBuilder.artifacts(artifacts);
        }
        if (dependencies != null) {
            moduleBuilder.dependencies(dependencies);
        }
        List<Module> modules = Arrays.asList(moduleBuilder.build());
        Build build = new Build();
        build.setModules(modules);
        return build;
    }

    protected String getModFilePath() {
        return path.toString() + File.separator + LOCAL_GO_MOD_FILENAME;
    }
}
