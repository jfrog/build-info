package org.jfrog.build.extractor.clientConfiguration;

/**
 * @author freds
 */
public interface ClientConfigurationFields {
    String NAME = "name";
    String USERNAME = "username";
    String HOST = "host";
    String PORT = "port";
    String NO_PROXY = "noProxy";
    String PASSWORD = "password";
    String MAVEN = "maven";
    String IVY = "ivy";
    String IVY_M2_COMPATIBLE = "ivy.m2compatible";
    String IVY_ART_PATTERN = "ivy.artPattern";
    String IVY_REPO_DEFINED = "ivy.repo.defined";
    String IVY_IVY_PATTERN = "ivy.ivyPattern";
    String PACKAGE_MANAGER_ARGS = "package.manager.args";
    String PACKAGE_MANAGER_PATH = "package.manager.path"; // Path to package-manager execution dir
    String PACKAGE_MANAGER_MODULE = "package.manager.module"; // Custom module name for the build-info
    String NPM_CI_COMMAND = "npm.ci.command"; // Determines whether the npm build is 'npm install' or 'npm ci' command.
    String GO_PUBLISHED_VERSION = "go.version"; // Version of the package published.
    String PIP_ENV_ACTIVATION = "pip.env.activation";
    String DOTNET_USE_DOTNET_CORE_CLI = "dotnet.use.dotnet.core.cli";
    String DOTNET_NUGET_PROTOCOL = "dotnet.nuget.protocol";
    String DOCKER_IMAGE_TAG = "docker.image.tag";
    String KANIKO_IMAGE_FILE = "kaniko.image.file";
    String JIB_IMAGE_FILE = "jib.image.file";
    String DOCKER_HOST = "docker.host";
    String URL = "url";
    String REPO_KEY = "repoKey";
    String SNAPSHOTS_DISABLED = "snapshots.disabled";
    String SNAPSHOT_UPDATE_POLICY = "snapshots.updatePolicy";
    String DOWN_SNAPSHOT_REPO_KEY = "downSnapshotRepoKey";
    // Publish fields
    String PUBLISH_ARTIFACTS = "artifacts";
    String PUBLISH_BUILD_INFO = "buildInfo";
    String PUBLISH_FORK_COUNT = "forkCount";
    String RECORD_ALL_DEPENDENCIES = "record.all.dependencies";
    String SNAPSHOT_REPO_KEY = "snapshot.repoKey";
    String RELEASE_REPO_KEY = "release.repoKey";
    String MATRIX = "matrix";
    String ARTIFACT_SPECS = "artifactSpecs";
    String INCLUDE_PATTERNS = "includePatterns";
    String EXCLUDE_PATTERNS = "excludePatterns";
    String FILTER_EXCLUDED_ARTIFACTS_FROM_BUILD = "filterExcludedArtifactsFromBuild";
    String EVEN_UNSTABLE = "unstable";
    String CONTEXT_URL = "contextUrl";
    String PUBLICATIONS = "publications";
    String ADD_DEPLOYABLE_ARTIFACTS = "add.deployable.artifacts";
}
