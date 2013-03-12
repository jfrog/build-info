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
import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater
import org.slf4j.Logger
import static org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask.BUILD_INFO_TASK_NAME

class ArtifactoryPlugin implements Plugin<Project> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
    public static final String ENCODING = "UTF-8"

    def void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.path}")
            return
        }
        // Add a singleton artifactory plugin convention to the root project if needed
        // Then add the build info task
        ArtifactoryPluginConvention conv = getArtifactoryPluginConvention(project)
        createBuildInfoTask(project)
        if (!conv.clientConfig.info.buildStarted) {
            conv.clientConfig.info.setBuildStarted(System.currentTimeMillis())
        }
        log.debug("Using Artifactory Plugin for ${project.path}")

        if (!conv.clientConfig.isBuildListernerAdded()) {
            def gradle = project.getGradle()
            gradle.addBuildListener(new ProjectsEvaluatedBuildListener())
            conv.clientConfig.setBuildListernerAdded(true)
        }
    }

    private static class ProjectsEvaluatedBuildListener extends BuildAdapter {
        def void projectsEvaluated(Gradle gradle) {
            ArtifactoryClientConfiguration configuration =
                ArtifactoryPluginUtil.getArtifactoryConvention(gradle.rootProject).getClientConfig()
            //Fill-in the client config for the global, then adjust children project
            GradleArtifactoryClientConfigUpdater.update(configuration, gradle.rootProject)
            gradle.rootProject.allprojects.each {
                //pass in the resolver of the cc
                defineResolvers(it, configuration.resolver)
            }
            //Configure the artifactoryPublish tasks. Deployment happens on task execution
            gradle.rootProject.getTasksByName(BUILD_INFO_TASK_NAME, true).each { BuildInfoTask bit ->
                bit.projectsEvaluated()
            }
        }

        private void defineResolvers(Project project, ResolverHandler resolverConf) {
            String url = resolverConf.getUrl()
            if (StringUtils.isNotBlank(url)) {
                log.debug("Artifactory URL: $url")
                // add artifactory url to the list of repositories
                if (resolverConf.isIvyRepositoryDefined() && resolverConf.isMaven()) {
                    createMavenRepo(project, url, resolverConf)
                    createIvyRepo(project, url, resolverConf)
                } else if (resolverConf.isMaven()) {
                    createMavenRepo(project, url, resolverConf)
                } else if (resolverConf.isIvyRepositoryDefined()) {
                    createIvyRepo(project, url, resolverConf)
                }
            } else {
                log.debug("No repository resolution defined for ${project.path}")
            }
        }

        private def createMavenRepo(Project project, String pUrl, ResolverHandler resolverConf) {
            return project.repositories.maven {
                name = 'artifactory-maven-resolver'
                url = resolverConf.urlWithMatrixParams(pUrl)
                if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                    credentials {
                        username = resolverConf.username
                        password = resolverConf.password
                    }
                }
            }
        }

        private def createIvyRepo(Project project, String pUrl, ResolverHandler resolverConf) {
            return project.repositories.ivy {
                name = 'artifactory-ivy-resolver'
                url = resolverConf.urlWithMatrixParams(pUrl)
                layout 'pattern', {
                    artifact resolverConf.getIvyArtifactPattern()
                    ivy resolverConf.getIvyPattern()
                }
                if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                    credentials {
                        username = resolverConf.username
                        password = resolverConf.password
                    }
                }
            }
        }
    }

    ArtifactoryPluginConvention getArtifactoryPluginConvention(Project project) {
        if (project.rootProject.convention.plugins.artifactory == null) {
            project.rootProject.convention.plugins.artifactory = new ArtifactoryPluginConvention(project)
        }
        return project.rootProject.convention.plugins.artifactory
    }

    BuildInfoTask createBuildInfoTask(Project project) {
        BuildInfoTask buildInfo = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        if (buildInfo == null) {
            def isRoot = project.equals(project.getRootProject())
            log.debug("Configuring buildInfo task for project ${project.path}: is root? ${isRoot}")
            buildInfo = project.getTasks().add(BUILD_INFO_TASK_NAME, BuildInfoTask.class)
            buildInfo.setDescription('''Deploys artifacts + generated build-info metadata to Artifactory, and resolves
dependencies from Artifactory.''')
            buildInfo.setGroup("publishing")
        }
        return buildInfo
    }
}

