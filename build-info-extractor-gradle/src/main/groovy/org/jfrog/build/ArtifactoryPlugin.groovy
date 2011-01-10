/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.build

import org.apache.commons.lang.StringUtils
import org.gradle.BuildAdapter

import org.gradle.api.Plugin
import org.gradle.api.Project

import org.gradle.api.invocation.Gradle

import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask
import org.slf4j.Logger
import static org.jfrog.build.ArtifactoryPluginUtils.BUILD_INFO_TASK_NAME
import static org.jfrog.build.api.BuildInfoFields.BUILD_STARTED
import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PREFIX

class ArtifactoryPlugin implements Plugin<Project> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
    public static final String ENCODING = "UTF-8"

    def void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.path}")
            return
        }
        // First add the build info task
        BuildInfoRecorderTask birt = createBuildInfoTask(project)
        def acc = birt.getArtifactoryClientConfiguration()
        Map startParamProps = project.gradle.getStartParameter().projectProperties
        if (!acc.info.buildStarted) {
            def start = "" + System.currentTimeMillis()
            startParamProps[BUILD_INFO_PREFIX + BUILD_STARTED] = start
        }
        log.debug("Using Artifactory Plugin for ${project.path}")
        defineResolvers(birt)
        String buildListenerAdded = startParamProps['__ArtifactoryPlugin_buildListener__'];
        if (!buildListenerAdded) {
            def gradle = project.getGradle()
            gradle.addBuildListener(new ProjectEvaluatedBuildListener())
            // Flag the last buildInfo task in the execution graph
            gradle.getTaskGraph().whenReady {
                boolean last = true
                gradle.getTaskGraph().getAllTasks().reverseEach {
                    if (BUILD_INFO_TASK_NAME.equals(it.name)) {
                        it.lastInGraph = last
                        last = false
                    }
                }
            }
            startParamProps['__ArtifactoryPlugin_buildListener__'] = 'done'
        }
    }

    private void defineResolvers(BuildInfoRecorderTask birt) {
        ArtifactoryClientConfiguration acc = birt.getArtifactoryClientConfiguration()
        String downloadId = acc.resolver.repoKey
        if (acc.resolver.isEnabled() && StringUtils.isNotBlank(downloadId)) {
            String artifactoryUrl = acc.contextUrl ?: 'http://gradle.artifactoryonline.com/gradle'
            while (artifactoryUrl.endsWith("/")) {
                artifactoryUrl = StringUtils.removeEnd(artifactoryUrl, "/")
            }
            def artifactoryDownloadUrl = acc.resolver.getDownloadUrl() ?: "${artifactoryUrl}/${downloadId}"
            log.debug("Artifactory URL: $artifactoryUrl")
            log.debug("Artifactory Download ID: $downloadId")
            log.debug("Artifactory Download URL: $artifactoryDownloadUrl")
            // add artifactory url to the list of repositories
            birt.getProject().repositories {
                mavenRepo urls: [artifactoryDownloadUrl]
            }
        } else {
            log.debug("No repository resolution defined for ${birt.getProject().path}")
        }
    }

    private static class ProjectEvaluatedBuildListener extends BuildAdapter {
        def void projectsEvaluated(Gradle gradle) {
            gradle.rootProject.getTasksByName(BUILD_INFO_TASK_NAME, true).each {
                it.projectsEvaluated()
            }
        }
    }

    BuildInfoRecorderTask createBuildInfoTask(Project project) {
        if (project.tasks.findByName(BUILD_INFO_TASK_NAME)) {
            return
        }
        def isRoot = project.equals(project.getRootProject())
        log.debug("Configuring buildInfo task for project ${project.name}: is root? ${isRoot}")
        BuildInfoRecorderTask buildInfo = project.getTasks().add(BUILD_INFO_TASK_NAME, BuildInfoRecorderTask.class)
        buildInfo.setDescription("Generates build info from build artifacts");
        return buildInfo
    }
}

