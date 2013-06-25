package org.jfrog.build.extractor.maven.plugin

import org.jfrog.build.api.util.NullLog
import org.jfrog.build.client.ArtifactoryClientConfiguration


class Config
{
    private static final ArtifactoryClientConfiguration CLIENT_CONFIGURATION = new ArtifactoryClientConfiguration( new NullLog())

    static class Resolver
    {
        @Delegate
        ArtifactoryClientConfiguration.ResolverHandler delegate = CLIENT_CONFIGURATION.resolver
    }

    static class Publisher
    {
        @Delegate
        ArtifactoryClientConfiguration.PublisherHandler delegate = CLIENT_CONFIGURATION.publisher
    }

    static class BuildInfo
    {
        @Delegate
        ArtifactoryClientConfiguration.BuildInfoHandler delegate = CLIENT_CONFIGURATION.info
    }
}
