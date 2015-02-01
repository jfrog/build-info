package org.jfrog.build.extractor.maven.plugin

import org.gcontracts.annotations.Ensures
import org.jfrog.build.api.util.NullLog
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.build.extractor.clientConfiguration.PrefixPropertyHandler


class Config
{
    private static final ArtifactoryClientConfiguration CLIENT_CONFIGURATION = new ArtifactoryClientConfiguration( new NullLog())

    interface DelegatesToPrefixPropertyHandler
    {
        @Ensures ({ result })
        PrefixPropertyHandler getDelegate()
    }

    static class Artifactory
    {
        @Delegate
        ArtifactoryClientConfiguration delegate = CLIENT_CONFIGURATION
    }

    static class Resolver implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.ResolverHandler delegate = CLIENT_CONFIGURATION.resolver
    }

    static class Publisher implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.PublisherHandler delegate = CLIENT_CONFIGURATION.publisher
    }

    static class BuildInfo implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.BuildInfoHandler delegate = CLIENT_CONFIGURATION.info
    }

    static class LicenseControl implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.LicenseControlHandler delegate = CLIENT_CONFIGURATION.info.licenseControl
    }

    static class IssuesTracker implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.IssuesTrackerHandler delegate = CLIENT_CONFIGURATION.info.issues
    }

    static class BlackDuck implements DelegatesToPrefixPropertyHandler
    {
        @Delegate
        ArtifactoryClientConfiguration.BlackDuckPropertiesHandler delegate = CLIENT_CONFIGURATION.info.blackDuckProperties
    }
}
