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

import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater

import static org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoTask.BUILD_INFO_TASK_NAME
import org.apache.commons.lang.StringUtils
import org.apache.ivy.plugins.resolver.DependencyResolver
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.IvyRepResolver
import org.gradle.BuildAdapter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.build.client.ArtifactoryClientConfiguration.ResolverHandler
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.slf4j.Logger

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
            //Config the tasks. Deploymenet happens on task execution
            gradle.rootProject.getTasksByName(BUILD_INFO_TASK_NAME, true).each {
                it.projectsEvaluated()
            }
        }

        private void defineResolvers(Project project, ResolverHandler resolverConf) {
            String url = resolverConf.getUrl()
            if (StringUtils.isNotBlank(url)) {
                log.debug("Artifactory URL: $url")
                // add artifactory url to the list of repositories
                if (resolverConf.isIvyRepositoryDefined()) {
                    addIvyRepoToProject(project, url, resolverConf)
                }
                if (resolverConf.isMaven()) {
                    project.repositories {
                        mavenRepo urls: [url]
                    }
                }
                if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                    String host = new URL(url).getHost()
                    org.apache.ivy.util.url.CredentialsStore.INSTANCE.addCredentials('Artifactory Realm', host, resolverConf.username, resolverConf.password);
                }
            } else {
                log.debug("No repository resolution defined for ${project.path}")
            }
            injectMatrixParamToResolvers(project.repositories.getAll(), resolverConf)
        }

        private def addIvyRepoToProject(Project project, String configuredUrl, ResolverHandler resolverConf) {
            project.repositories {
                add(new org.apache.ivy.plugins.resolver.URLResolver()) {
                    name = "ivy-resolver"
                    url = configuredUrl
                    m2compatible = resolverConf.m2Compatible
                    addArtifactPattern(configuredUrl + '/' + resolverConf.getIvyArtifactPattern())
                    addIvyPattern(configuredUrl + '/' + resolverConf.getIvyPattern())
                }
            }
        }

        private def injectMatrixParamToResolvers(Set<DependencyResolver> allResolvers,
                ArtifactoryClientConfiguration.ResolverHandler resolverConf) {
            for (DependencyResolver resolver: allResolvers) {
                //Change the resolver URLs to include matrix params
                if (resolver instanceof IvyRepResolver) {
                    resolver.artroot = resolverConf.urlWithMatrixParams(resolver.artroot)
                    resolver.ivyroot = resolverConf.urlWithMatrixParams(resolver.ivyroot)
                } else if (resolver instanceof IBiblioResolver) {
                    resolver.root = resolverConf.urlWithMatrixParams(resolver.root)
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
            buildInfo.setDescription('''Deploys artifacts + generated build-info metadata to Artifactiory, and resolves
depnedncies from Artifactory.''')
        }
        return buildInfo
    }
}

