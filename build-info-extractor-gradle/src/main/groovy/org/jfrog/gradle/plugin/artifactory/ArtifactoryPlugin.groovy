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
import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoRecorderTask
import org.jfrog.gradle.plugin.artifactory.extractor.GradlePluginUtils
import org.slf4j.Logger
import static org.jfrog.gradle.plugin.artifactory.extractor.GradlePluginUtils.BUILD_INFO_TASK_NAME

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
        if (!info.clientConfig.info.buildStarted) {
            info.clientConfig.info.setBuildStarted(System.currentTimeMillis())
        }
        log.debug("Using Artifactory Plugin for ${project.path}")

        if (!info.clientConfig.isBuildListernerAdded()) {
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
            info.clientConfig.setBuildListernerAdded(true)
        }
    }


    private static class ProjectEvaluatedBuildListener extends BuildAdapter {
        def void projectsEvaluated(Gradle gradle) {
            ArtifactoryClientConfiguration configuration = GradlePluginUtils.getArtifactoryConvention(gradle.rootProject).getClientConfig()
            GradlePluginUtils.fillArtifactoryClientConfiguration(configuration, gradle.rootProject)
            gradle.rootProject.allprojects.each {
                defineResolvers(it, configuration.resolver)
            }
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
            injectMatrixParamExistingResolvers(project.repositories.getAll(), resolverConf)
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

        private def injectMatrixParamExistingResolvers(Set<DependencyResolver> allResolvers, ArtifactoryClientConfiguration.ResolverHandler resolverConf) {
            for (DependencyResolver resolver: allResolvers) {
                if (resolver instanceof IvyRepResolver) {
                    resolver.artroot = resolverConf.injectMatrixParams(resolver.artroot)
                    resolver.ivyroot = resolverConf.injectMatrixParams(resolver.ivyroot)
                } else if (resolver instanceof IBiblioResolver) {
                    resolver.root = resolverConf.injectMatrixParams(resolver.root)
                }
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
            buildInfo.setDescription('''Deploys artifacts + generated build-info metadata to Artifactiory, and resolves
depnedncies from Artifactory.''')
        }
        return buildInfo
    }
}

