package org.jfrog.gradle.plugin.artifactory;

import org.gradle.api.Project;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;

public class ArtifactoryPluginUtil {

    public static ArtifactoryPluginConvention getArtifactoryConvention(Project project) {
        return project.getRootProject().getConvention().findPlugin(ArtifactoryPluginConvention.class);
    }

    public static ArtifactoryPluginConvention getPublisherConvention(Project project) {
        while (project != null) {
            ArtifactoryPluginConvention acc = project.getConvention().findPlugin(ArtifactoryPluginConvention.class);
            if (acc != null) {
                ArtifactoryClientConfiguration.PublisherHandler publisher = acc.getClientConfig().publisher;
                if (publisher.getContextUrl() != null && (publisher.getRepoKey() != null || publisher.getSnapshotRepoKey() != null)) {
                    return acc;
                }
            }
            project = project.getParent();
        }
        return null;
    }

    public static ArtifactoryClientConfiguration.PublisherHandler getPublisherHandler(Project project) {
        ArtifactoryPluginConvention convention = getPublisherConvention(project);
        if (convention != null) {
            return convention.getClientConfig().publisher;
        }
        return null;
    }

    public static ArtifactoryClientConfiguration.ResolverHandler getResolverHandler(Project project) {
        while (project != null) {
            ArtifactoryPluginConvention acc = project.getConvention().findPlugin(ArtifactoryPluginConvention.class);
            if (acc != null) {
                ArtifactoryClientConfiguration.ResolverHandler resolver = acc.getClientConfig().resolver;
                if (resolver.getContextUrl() != null && resolver.getRepoKey() != null) {
                    return resolver;
                }
            }
            project = project.getParent();
        }
        return null;
    }
}