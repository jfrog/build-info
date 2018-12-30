/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
                if (publisher.getContextUrl() != null && publisher.getRepoKey() != null) {
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