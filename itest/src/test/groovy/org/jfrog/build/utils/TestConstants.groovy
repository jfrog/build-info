package org.jfrog.build.utils

/**
 * @author Aviad Shikloshi
 */
interface TestConstants {

    static final String repoKey = "artifactory.publish.repoKey"
    static final String snapshotRepoKey = "artifactory.publish.snapshot.repoKey"
    static final String resolveRepokey = "artifactory.resolve.repoKey"
    static final String resolveSnapshotKey = "artifactory.resolve.downSnapshotRepoKey"
    static final String artifactoryUrl = "artifactory.publish.contextUrl"
    static final String artifactoryResolveUrl = "artifactory.resolve.contextUrl"
    static final String artifactoryDeployBuildName = "artifactory.deploy.build.name"
    static final String artifactoryPublishPassword = "artifactory.publish.password"
    static final String artifactoryPublishUsername = "artifactory.publish.username"
    static final String artifactoryResolvePassword = "artifactory.resolve.password"
    static final String artifactoryResolveUsername = "artifactory.resolve.username"
    static final String deployTimestamp = "artifactory.deploy.build.timestamp"
    static final String shouldPublishArtifacts = "artifactory.publish.artifacts"

    static final String buildName = "buildInfo.build.name"
    static final String buildNumber = "buildInfo.build.number"
    static final String buildTimestamp = "buildInfo.build.timestamp"
    static final String buildStarted = "buildInfo.build.started"
    static final String started = "buildInfo.build.started"

    static final String includeEnvVars = "buildInfoConfig.includeEnvVars"

}