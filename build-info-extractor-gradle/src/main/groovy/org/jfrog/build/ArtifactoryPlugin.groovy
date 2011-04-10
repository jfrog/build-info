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
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.IvyRepResolver
import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jfrog.build.client.ArtifactoryClientConfiguration.ResolverHandler
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask

import org.slf4j.Logger
import static org.jfrog.build.ArtifactoryPluginUtils.BUILD_INFO_TASK_NAME
import org.jfrog.dsl.ArtifactoryPluginConvention

class ArtifactoryPlugin implements Plugin<Project> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
    public static final String ENCODING = "UTF-8"

    def void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.path}")
            return
        }
        // First add the build info config task to the root project if needed
        // Then add the build info task
        ArtifactoryPluginConvention info = getArtifactoryPluginInfo(project)
        createBuildInfoTask(project)
        if (!info.configuration.info.buildStarted) {
            info.configuration.info.buildStarted = "" + System.currentTimeMillis()
        }
        log.debug("Using Artifactory Plugin for ${project.path}")
        defineResolvers(project, info.configuration.resolver)

        if (!info.configuration.isBuildListernerAdded()) {
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
            info.configuration.setBuildListernerAdded(true)
        }
    }

    private void defineResolvers(Project project, ResolverHandler resolverConf) {
        String downloadId = resolverConf.repoKey
        if (resolverConf.isEnabled() && StringUtils.isNotBlank(downloadId)) {
            String artifactoryUrl = resolverConf.contextUrl ?: 'http://gradle.artifactoryonline.com/gradle'
            while (artifactoryUrl.endsWith("/")) {
                artifactoryUrl = StringUtils.removeEnd(artifactoryUrl, "/")
            }
            def artifactoryDownloadUrl = resolverConf.getDownloadUrl() ?: "${artifactoryUrl}/${downloadId}"
            log.debug("Artifactory URL: $artifactoryUrl")
            log.debug("Artifactory Download ID: $downloadId")
            log.debug("Artifactory Download URL: $artifactoryDownloadUrl")
            // add artifactory url to the list of repositories
            project.repositories {
                mavenRepo urls: [artifactoryDownloadUrl]
            }
        } else {
            log.debug("No repository resolution defined for ${project.path}")
        }
        String buildRoot = resolverConf.getBuildRoot()
        if (StringUtils.isNotBlank(buildRoot)) {
            injectPropertyIntoExistingResolvers(project.repositories.getAll(), buildRoot)
        }
    }

    private def injectPropertyIntoExistingResolvers(Set<DependencyResolver> allResolvers, String buildRoot) {
        for (DependencyResolver resolver: allResolvers) {
            if (resolver instanceof IvyRepResolver) {
                resolver.artroot = StringUtils.removeEnd(resolver.artroot, '/') + ';' + buildRoot + ';'
                resolver.ivyroot = StringUtils.removeEnd(resolver.ivyroot, '/') + ';' + buildRoot + ';'
            } else if (resolver instanceof IBiblioResolver) {
                resolver.root = StringUtils.removeEnd(resolver.root, '/') + ';' + buildRoot + ';'
            }
        }
    }

    private static class ProjectEvaluatedBuildListener extends BuildAdapter {
        def void projectsEvaluated(Gradle gradle) {
            gradle.rootProject.getTasksByName(BUILD_INFO_TASK_NAME, true).each {
                it.projectsEvaluated()
            }
        }
    }

    ArtifactoryPluginConvention getArtifactoryPluginInfo(Project project) {
        if (project.rootProject.convention.plugins.artifactory == null) {
            project.rootProject.convention.plugins.artifactory = new ArtifactoryPluginConvention(project)
        }
        return project.rootProject.convention.plugins.artifactory
    }

    BuildInfoRecorderTask createBuildInfoTask(Project project) {
        BuildInfoRecorderTask buildInfo = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        if (buildInfo == null) {
            def isRoot = project.equals(project.getRootProject())
            log.debug("Configuring buildInfo task for project ${project.path}: is root? ${isRoot}")
            buildInfo = project.getTasks().add(BUILD_INFO_TASK_NAME, BuildInfoRecorderTask.class)
            buildInfo.setDescription("Generates build info from build artifacts")
        }
        return buildInfo
    }
}

