package org.jfrog.build.extractor.ci;

/**
 * @author freds
 */
public interface BuildInfoFields {
    String BUILD_NAME = "build.name";
    String BUILD_NUMBER = "build.number";
    String BUILD_PROJECT = "build.project";
    String BUILD_TIMESTAMP = "build.timestamp";
    String BUILD_STARTED = "build.started";
    String ARTIFACTORY_PLUGIN_VERSION = "artifactoryPluginVersion";
    String BUILD_PARENT_NAME = "build.parentName";
    String BUILD_PARENT_NUMBER = "build.parentNumber";
    String VCS_REVISION = "vcs.revision";
    String VCS_URL = "vcs.url";
    String VCS_BRANCH = "vcs.branch";
    String VCS_MESSAGE = "vcs.message";
    String PRINCIPAL = "principal";
    String BUILD_URL = "buildUrl";
    String BUILD_AGENT_NAME = "buildAgent.name";
    String BUILD_AGENT_VERSION = "buildAgent.version";
    String AGENT_NAME = "agent.name";
    String AGENT_VERSION = "agent.version";
    String OUTPUT_FILE = "output.file";
    String ENVIRONMENT_PREFIX = "env.";
    String BUILD_RETENTION_DAYS = "buildRetention.daysToKeep";
    String BUILD_RETENTION_COUNT = "buildRetention.count";
    String DELETE_BUILD_ARTIFACTS = "buildRetention.deleteBuildArtifacts";
    String BUILD_RETENTION_ASYNC = "buildRetention.async";
    String BUILD_NUMBERS_NOT_TO_DELETE = "buildRetention.buildNumbersNotToDelete";
    String BUILD_RETENTION_MINIMUM_DATE = "buildRetention.minimumDate";
    String RELEASE_ENABLED = "promotion.enabled";
    String RELEASE_COMMENT = "promotion.comment";
    String BUILD_ROOT = "build.root";
    String RUN_PARAMETERS = "runParameters.";
    String INCREMENTAL = "incremental";
    String GENERATED_BUILD_INFO = "generated.build.info";
    String VCS = "vcs";
    String DEPLOYABLE_ARTIFACTS = "deployable.artifacts.map";
    String MIN_CHECKSUM_DEPLOY_SIZE_KB = "minChecksumDeploySizeKb";
    // Backward compatibility for pipelines using Gradle Artifactory Plugin with version bellow 4.15.1, or Jenkins Artifactory Plugin bellow 3.6.1
    @Deprecated
    String BACKWARD_COMPATIBLE_DEPLOYABLE_ARTIFACTS = "deployable.artifacts";
}
