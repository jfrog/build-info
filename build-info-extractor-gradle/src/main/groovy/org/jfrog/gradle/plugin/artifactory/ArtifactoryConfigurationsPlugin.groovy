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

package org.jfrog.gradle.plugin.artifactory

import org.apache.commons.lang.StringUtils
import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.build.client.ArtifactoryClientConfiguration.ResolverHandler
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoConfigurationsTask
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoConfigurationsTask
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoPublicationsTask
import org.slf4j.Logger
import static BuildInfoConfigurationsTask.BUILD_INFO_TASK_NAME

class ArtifactoryConfigurationsPlugin extends ArtifactoryPluginBase {

    @Override
    protected ArtifactoryPluginConvention createArtifactoryPluginConvention(Project project) {
        return new ArtifactoryPluginConvention(project)
    }

    @Override
    protected BuildInfoBaseTask createArtifactoryPublishTask(Project project) {
        def result = project.getTasks().create(BUILD_INFO_TASK_NAME, BuildInfoConfigurationsTask.class)
        result.setDescription('''Deploys artifacts + generated build-info metadata to Artifactory, using project configurations.''')
        return result
    }
}

