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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.Upload
import org.jfrog.build.client.ClientGradleProperties
import org.jfrog.build.client.ClientIvyProperties
import org.jfrog.build.client.ClientProperties
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask
import org.slf4j.Logger
import static org.jfrog.build.ArtifactoryPluginUtils.BUILD_INFO_TASK_NAME

class ArtifactoryPlugin implements Plugin<Project> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
    public static final String ENCODING = "UTF-8"

    def void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.path}")
            return
        }
        Map startParamProps = project.gradle.getStartParameter().projectProperties
        String buildStart = startParamProps['build.start'];
        if (!buildStart) {
            startParamProps['build.start'] = "" + System.currentTimeMillis()
        }
        log.debug("Using Artifactory Plugin for ${project.path}")
        defineResolvers(project)
        // add the build info task
        createBuildInfoTask(project);
        String buildListenerAdded = startParamProps['__ArtifactoryPlugin_buildListener__'];
        if (!buildListenerAdded) {
            project.getGradle().addBuildListener(new ProjectEvaluatedBuildListener())
            startParamProps['__ArtifactoryPlugin_buildListener__'] = 'done'
        }
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

    private static class ProjectEvaluatedBuildListener extends BuildAdapter {
        def void projectsEvaluated(Gradle gradle) {
            gradle.rootProject.allprojects.each {
                BuildInfoRecorderTask buildInfoTask = it.tasks.findByName(BUILD_INFO_TASK_NAME)
                if (buildInfoTask != null) configureBuildInfoTask(it, buildInfoTask)
            }
        }

        void configureBuildInfoTask(Project project, BuildInfoRecorderTask buildInfoTask) {
            TaskContainer tasks = project.getTasks()
            if (buildInfoTask.getConfiguration() == null) {
                buildInfoTask.configuration = project.configurations.findByName(Dependency.ARCHIVES_CONFIGURATION)
            }
            project.subprojects.each { if (it.tasks.findByName(BUILD_INFO_TASK_NAME)) buildInfoTask.dependsOn(it.buildInfo) }

            // If no configuration no descriptor
            if (buildInfoTask.configuration == null) {
                return
            }

            // Set ivy descriptor parameters
            if (ArtifactoryPluginUtils.getBooleanProperty(ClientIvyProperties.PROP_PUBLISH_IVY, project) &&
                    buildInfoTask.ivyDescriptor == null) {
                // Flag to publish the Ivy XML file, but no ivy descriptor file inputted, activate default upload${configuration}.
                Upload uploadTask = tasks.getByName(buildInfoTask.configuration.getUploadTaskName())
                if (!uploadTask.isUploadDescriptor()) {
                    throw new GradleException("""Cannot publish Ivy descriptor if ivyDescriptor not set in task: ${buildInfoTask.path}
                    And flag uploadDescriptor not set in default task: ${uploadTask.path}""")
                }
                buildInfoTask.ivyDescriptor = uploadTask.descriptorDestination
                buildInfoTask.dependsOn(uploadTask)
            } else {
                buildInfoTask.ivyDescriptor = null
            }

            // Set maven pom parameters
            if (ArtifactoryPluginUtils.getBooleanProperty(ClientGradleProperties.PROP_PUBLISH_MAVEN, project) &&
                    buildInfoTask.mavenDescriptor == null) {
                // Flag to publish the Maven POM, but no pom file inputted, activate default Maven install.
                // if the project doesn't have the maven install task, throw an exception
                Upload installTask = tasks.withType(Upload.class).findByName('install')
                if (installTask == null) {
                    throw new GradleException("""Cannot publish Maven descriptor if mavenDescriptor not set in task: ${buildInfoTask.path}
                    And default install task for project ${project.path} is not an Upload task""")
                }
                buildInfoTask.mavenDescriptor = new File(project.getRepositories().getMavenPomDir(), "pom-default.xml")
                buildInfoTask.mavenPom = project.getRepositories().mavenDeployer().getPom()
                buildInfoTask.dependsOn(installTask)
            } else {
                buildInfoTask.mavenDescriptor = null
            }
        }
    }

    void createBuildInfoTask(Project project) {
        if (project.tasks.findByName(BUILD_INFO_TASK_NAME)) {
            return
        }
        def isRoot = project.equals(project.getRootProject())
        log.debug("Configuring buildInfo task for project ${project.name}: is root? ${isRoot}")
        BuildInfoRecorderTask buildInfo = project.getTasks().add(BUILD_INFO_TASK_NAME, BuildInfoRecorderTask.class)
        buildInfo.setDescription("Generates build info from build artifacts");
    }
}

