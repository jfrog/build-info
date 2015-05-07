package org.jfrog.build.utils

/**
 * @author Aviad Shikloshi
 */
interface TestConstants {

    static final String repoKey = "artifactory.publish.repoKey"
    static final String shouldPublishArtifacts = "artifactory.publish.artifacts"
    static final String buildName = "buildInfo.build.name"
    static final String buildNumber = "buildInfo.build.number"
    static final String artifactoryUrl = "artifactory.publish.contextUrl"
    static final String includeEnvVars = "buildInfoConfig.includeEnvVars"

}