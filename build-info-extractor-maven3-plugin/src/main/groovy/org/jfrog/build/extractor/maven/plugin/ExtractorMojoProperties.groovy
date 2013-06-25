package org.jfrog.build.extractor.maven.plugin

import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.gmaven.mojo.GroovyMojo


/**
 * Base Mojo class holding all parameters and their mapping to properties.
 */
@SuppressWarnings([ 'GroovyInstanceVariableNamingConvention' ])
abstract class ExtractorMojoProperties extends GroovyMojo
{
    @Parameter
    @Property( name = 'org.jfrog.build.extractor.maven.recorder.activate', defaultValue = 'true' )
    String orgJfrogBuildExtractorMavenRecorderActivate

    @Parameter
    @Property( name = 'artifactory.publish.contextUrl' )
    String artifactoryPublishContextUrl

    @Parameter
    @Property( name = 'artifactory.publish.username' )
    String artifactoryPublishUsername

    @Parameter
    @Property( name = 'artifactory.publish.password' )
    String artifactoryPublishPassword

    @Parameter
    @Property( name = 'artifactory.publish.resolution.repoKey' )
    String artifactoryResolutionRepoKey

    @Parameter
    @Property( name = 'artifactory.publish.repoKey' )
    String artifactoryPublishRepoKey

    @Parameter
    @Property( name = 'artifactory.publish.snapshot.repoKey' )
    String artifactoryPublishSnapshotRepoKey

    @Parameter
    @Property( name = 'artifactory.publish.artifacts', defaultValue = 'true' )
    String artifactoryPublishArtifacts

    @Parameter
    @Property( name = 'artifactory.publish.buildInfo', defaultValue = 'true' )
    String artifactoryPublishBuildInfo

    @Parameter
    @Property( name = 'artifactory.publish.maven', defaultValue = 'false' )
    String artifactoryPublishMaven

    @Parameter
    @Property( name = 'artifactory.publish.ivy', defaultValue = 'false' )
    String artifactoryPublishIvy

    @Parameter
    @Property( name = 'artifactory.publish.ivy.m2compatible', defaultValue = 'false' )
    String artifactoryPublishIvyM2compatible

    @Parameter
    @Property( name = 'buildInfoConfig.includeEnvVars', defaultValue = 'false' )
    String buildInfoConfigIncludeEnvVars

    @Parameter
    @Property( name = 'artifactory.publish.unstable', defaultValue = 'false' )
    String artifactoryPublishUnstable

    @Parameter
    @Property( name = 'buildInfo.buildRetention.deleteBuildArtifacts', defaultValue = 'false' )
    String buildInfoBuildRetentionDeleteBuildArtifacts

    @Parameter
    @Property( name = 'buildInfo.buildRetention.daysToKeep', defaultValue = '3' )
    String buildInfoBuildRetentionDaysToKeep

    @Parameter
    @Property( name = 'buildInfo.licenseControl.includePublishedArtifacts', defaultValue = 'false' )
    String buildInfoLicenseControlIncludePublishedArtifacts

    @Parameter
    @Property( name = 'buildInfo.licenseControl.violationRecipients' )
    String buildInfoLicenseControlViolationRecipients

    @Parameter
    @Property( name = 'buildInfo.licenseControl.scopes' )
    String buildInfoLicenseControlScopes

    @Parameter
    @Property( name = 'buildInfo.governance.blackduck.runChecks', defaultValue = 'false' )
    String buildInfoGovernanceBlackduckRunChecks

    @Parameter
    @Property( name = 'buildInfo.governance.blackduck.includePublishedArtifacts', defaultValue = 'false' )
    String buildInfoGovernanceBlackduckIncludePublishedArtifacts

    @Parameter
    @Property( name = 'buildInfo.governance.blackduck.autoCreateMissingComponentRequests', defaultValue = 'false' )
    String buildInfoGovernanceBlackduckAutoCreateMissingComponentRequests

    @Parameter
    @Property( name = 'buildInfo.governance.blackduck.autoDiscardStaleComponentRequests', defaultValue = 'false' )
    String buildInfoGovernanceBlackduckAutoDiscardStaleComponentRequests

    @Parameter
    @Property( name = 'buildInfo.licenseControl.runChecks', defaultValue = 'false' )
    String buildInfoLicenseControlRunChecks

    @Parameter
    @Property( name = 'buildInfo.licenseControl.autoDiscover', defaultValue = 'false' )
    String buildInfoLicenseControlAutoDiscover

    @Parameter
    @Property( name = 'artifactory.publish.includePatterns' )
    String artifactoryPublishIncludePatterns

    @Parameter
    @Property( name = 'artifactory.publish.excludePatterns' )
    String artifactoryPublishExcludePatterns

    @Parameter
    @Property( name = 'buildInfoConfig.envVarsIncludePatterns' )
    String buildInfoConfigEnvVarsIncludePatterns

    @Parameter
    @Property( name = 'buildInfoConfig.envVarsExcludePatterns', defaultValue = '*password*,*secret*' )
    String buildInfoConfigEnvVarsExcludePatterns

    @Parameter
    @Property( name = 'artifactory.timeout', defaultValue = '300' )
    String artifactoryTimeout

    @Parameter
    @Property( name = 'buildInfoConfig.propertiesFile', defaultValue = '300' )
    String buildInfoConfigPropertiesFile

    @Parameter
    @Property( name = 'buildInfo.build.root' )
    String buildInfoBuildRoot

    @Parameter
    @Property( name = 'artifactory.resolve.matrixbuild.root' )
    String artifactoryResolveMatrixbuildRoot

    @Parameter
    @Property( name = 'artifactory.deploy.build.root' )
    String artifactoryDeployBuildRoot

    @Parameter
    @Property( name = 'buildInfo.build.name' )
    String buildInfoBuildName

    @Parameter
    @Property( name = 'artifactory.deploy.build.name' )
    String artifactoryDeployBuildName

    @Parameter
    @Property( name = 'buildInfo.build.timestamp' )
    String buildInfoBuildTimestamp

    @Parameter
    @Property( name = 'buildInfo.build.started' )
    String buildInfoBuildStarted

    @Parameter
    @Property( name = 'artifactory.deploy.build.timestamp' )
    String artifactoryDeployBuildTimestamp

    @Parameter
    @Property( name = 'buildInfo.buildUrl' )
    String buildInfoBuildUrl

    @Parameter
    @Property( name = 'artifactory.deploy.build.number' )
    String artifactoryDeployBuildNumber

    @Parameter
    @Property( name = 'buildInfo.build.number' )
    String buildInfoBuildNumber

    @Parameter
    @Property( name = 'buildInfo.agent.name' )
    String buildInfoAgentName

    @Parameter
    @Property( name = 'buildInfo.agent.version' )
    String buildInfoAgentVersion

    @Parameter
    @Property( name = 'buildInfo.vcs.revision' )
    String buildInfoVcsRevision

    @Parameter
    @Property( name = 'artifactory.deploy.vcs.revision' )
    String artifactoryDeployVcsRevision

    /**
     * Inline properties to attach to all published artifacts
     */
    @Parameter
    @Property( name = 'artifactory.deploy' )
    String deployProperties
}
