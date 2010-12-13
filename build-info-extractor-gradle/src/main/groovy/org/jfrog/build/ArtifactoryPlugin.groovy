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
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.invocation.Gradle
import org.jfrog.build.client.ClientProperties
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask
import org.slf4j.Logger

class ArtifactoryPlugin implements Plugin<Project> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
    public static final String ENCODING = "UTF-8"

    private BuildAdapter buildInfoProjectEvaluatedListener = null

    def void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.name}")
            return
        }
        log.debug("Using Artifactory Plugin for ${project.name}")
        defineResolvers(project)
        // add the build info task
        configureBuildInfoTask(project);
        project.getGradle().addBuildListener(getBuildListener())
    }

    private void defineResolvers(Project project) {
        String artifactoryUrl = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_CONTEXT_URL, project) ?: 'http://gradle.artifactoryonline.com/gradle'
        while (artifactoryUrl.endsWith("/")) {
            artifactoryUrl = StringUtils.removeEnd(artifactoryUrl, "/")
        }
        String downloadId = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_RESOLVE_REPOKEY, project)
        if (StringUtils.isNotBlank(downloadId)) {
            def artifactoryDownloadUrl = ArtifactoryPluginUtils.getProperty('artifactory.downloadUrl', project) ?: "${artifactoryUrl}/${downloadId}"
            log.debug("Artifactory URL: $artifactoryUrl")
            log.debug("Artifactory Download ID: $downloadId")
            log.debug("Artifactory Download URL: $artifactoryDownloadUrl")
            // add artifactory url to the list of repositories
            project.repositories {
                mavenRepo urls: [artifactoryDownloadUrl]
            }
        } else {
            log.debug("No repository resolution defined for ${project.name}")
        }
    }

    private BuildAdapter getBuildListener() {
        if (buildInfoProjectEvaluatedListener != null) {
            return buildInfoProjectEvaluatedListener
        }
        buildInfoProjectEvaluatedListener = new BuildAdapter() {
            def void projectsEvaluated(Gradle gradle) {
                Map startParamProps = gradle.getStartParameter().projectProperties
                String buildStart = startParamProps['build.start'];
                if (!buildStart) {
                    startParamProps['build.start'] = ""+System.currentTimeMillis()
                }
                gradle.rootProject.allprojects {
                    BuildInfoRecorderTask buildInfoTask = tasks.findByName('buildInfo')
                    if (buildInfoTask != null) {
                        if (buildInfoTask.getConfiguration() == null) {
                            buildInfoTask.setConfiguration(getConfigurations().findByName(Dependency.ARCHIVES_CONFIGURATION))
                        }
                        Set<Object> dependsOnTasks = buildInfoTask.getDependsOn()
                        Task jarTask = tasks.findByName('jar')
                        if ((dependsOnTasks == null || dependsOnTasks.isEmpty()) && jarTask != null) {
                            buildInfoTask.dependsOn(jarTask)
                        }
                        if (subprojects != null && !subprojects.isEmpty()) {
                            subprojects.each { if (it.tasks.findByName('buildInfo')) buildInfoTask.dependsOn(it.buildInfo) }
                        }
                    }
                }
            }
        };
        return buildInfoProjectEvaluatedListener
    }

    private void configureBuildInfoTask(Project project) {
        if (project.tasks.findByName("buildInfo")) {
            return
        }
        def isRoot = project.equals(project.getRootProject())
        log.debug("Configuring buildInfo task for project ${project.name}: is root? ${isRoot}")
        BuildInfoRecorderTask buildInfo = project.getTasks().add("buildInfo", BuildInfoRecorderTask.class)
        buildInfo.setDescription("Generates build info from build artifacts");
    }
}

