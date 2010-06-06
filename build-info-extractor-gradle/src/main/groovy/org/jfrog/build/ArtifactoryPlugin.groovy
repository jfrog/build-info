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

import org.gradle.api.tasks.bundling.Jar

import org.apache.commons.lang.StringUtils
import org.gradle.BuildAdapter
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.invocation.Gradle
import org.gradle.api.plugins.PluginContainer
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Upload
import org.jfrog.build.api.BuildInfoConfigProperties
import org.jfrog.build.api.BuildInfoProperties
import org.jfrog.build.client.ClientProperties
import org.jfrog.build.client.DeploymentUrlUtils
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask
import org.slf4j.Logger

class ArtifactoryPlugin implements Plugin<Project> {
  private static final Logger log = org.slf4j.LoggerFactory.getLogger(ArtifactoryPlugin.class);
  public static final String ENCODING = "UTF-8"

  List<String> compatiblePlugins = ['java', 'scala', 'groovy'] as List

  def void apply(Project project) {
    log.debug("Using Artifactory Plugin")
    String artifactoryUrl = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_CONTEXT_URL, project) ?: 'http://gradle.artifactoryonline.com/gradle'
    while (artifactoryUrl.endsWith("/")) {
      artifactoryUrl = StringUtils.removeEnd(artifactoryUrl, "/")
    }
    def downloadId = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_RESOLVE_REPOKEY, project)
    if (!downloadId) {
      // take the target repository from the full url
      String[] pathParts = artifactoryUrl.split("/")
      if (pathParts.size() >= 3) {
        downloadId = pathParts[2]
      }
      //TODO: [by ys] why plugins releases in the default?
      downloadId = downloadId ?: 'plugins-releases'
    }
    def artifactoryDownloadUrl = ArtifactoryPluginUtils.getProperty('artifactory.downloadUrl', project) ?: "${artifactoryUrl}/${downloadId}"
    log.debug("Artifactory URL: $artifactoryUrl")
    log.debug("Artifactory Download ID: $downloadId")
    log.debug("Artifactory Download URL: $artifactoryDownloadUrl")
    // add artifactory url to the list of repositories
    project.repositories {
      mavenRepo urls: [artifactoryDownloadUrl]
    }

    log.debug("Configuring BuildInfo task")

    PluginContainer plugins = project.getPlugins();
    def isRoot = project.equals(project.getRootProject())
    log.debug "Configuring project ${project.name}: is root? ${isRoot}"
    // add the build info task only to supported projects (always include the root)
    plugins.matching(new Spec<Plugin>() {
      boolean isSatisfiedBy(Plugin plugin) {
        log.debug "Has plugin ${plugin.class} Is isSatisfied? ${compatiblePlugins.contains(plugin.class) || isRoot}"
        for (String pluginId: compatiblePlugins) {
          if (plugins.hasPlugin(pluginId)) {
            return true
          }
        }
        if (isRoot) {
          return true
        }
        return false
      }
    }).allPlugins(new Action<Plugin>() {
      public void execute(Plugin plugin) {
        log.debug "Configuring BuildInfoTask for project ${project.name} because of plugin ${plugin.class}"
        configureBuildInfoTask(project);
      }
    });

    def uploadId = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, project)
    if (uploadId) {
      // configure upload repository for maven deployer or ivy publisher
      log.debug "ArtifactoryURL before setting upload is ${artifactoryUrl}"
      log.debug("Upload ID was declared: ${uploadId} and so deployArchives task reconfigured")
      String uploadRootUrl = ArtifactoryPluginUtils.getProperty("artifactory.uploadRootUrl", project)
      String uploadUrl = "${artifactoryUrl}/${uploadId}"
      if (uploadRootUrl) {
        log.debug "Using Artifactory Upload Root URL: $uploadRootUrl"
        uploadUrl = "${uploadRootUrl}/${uploadId}"
      }

      log.debug("Configure Upload URL to ${uploadUrl}")
      def user = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_USERNAME, project) ?: "anonymous"
      def password = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_PASSWORD, project) ?: ""
      def host = new URI(uploadUrl).getHost()
      uploadUrl = appendProperties(uploadUrl, project)
      project.tasks.withType(Upload.class).allObjects { uploadTask ->
        project.configure(uploadTask) {
          boolean deployIvy
          def deployIvyProp = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_IVY, project);
          if (deployIvyProp != null) {
            deployIvy = Boolean.parseBoolean(deployIvyProp);
          } else {
            deployIvy = true
          }
          if (deployIvy) {
            log.debug "Configuring Ivy repository"
            repositories {
              add(new org.apache.ivy.plugins.resolver.URLResolver()) {
                org.apache.ivy.util.url.CredentialsStore.INSTANCE.addCredentials("Artifactory Realm", host, user, password)
                name = 'artifactory'
                addIvyPattern "$uploadUrl/[organisation]/[module]/[revision]/ivy-[revision].xml"
                addArtifactPattern "$uploadUrl/[organisation]/[module]/[revision]/[module]-[revision](-[classifier]).[ext]"
                descriptor = 'optional'
                checkmodified = true
                m2compatible = true
              }
            }
          }
          boolean deployMaven
          def deployMavenProp = ArtifactoryPluginUtils.getProperty(ClientProperties.PROP_PUBLISH_MAVEN, project);
          if (deployMavenProp != null) {
            deployMaven = Boolean.parseBoolean(deployMavenProp);
          } else {
            deployMaven = true
          }
          if (deployMaven) {
            log.debug "Configuring Maven repository"
            repositories.mavenDeployer {
              repository(url: uploadUrl) {
                authentication(userName: user, password: password)
              }
            }
          }
        }
      }
    } else {
      if (project.getRootProject().equals(project)) {
        log.warn "Upload ID was not declared, no actual deployment will be performed."
      }
    }
    project.getGradle().addBuildListener(new BuildAdapter() {
      def void projectsEvaluated(Gradle gradle) {
        String buildStart = System.getProperty("build.start");
        if (!buildStart) {
          System.setProperty("build.start", Long.toString(System.currentTimeMillis()));
        }
      }
    })
  }

  String appendProperties(String uploadUrl, Project project) {
    Properties props = new Properties()
    props.putAll(System.getProperties())
    String buildNumber = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER, project)
    if (buildNumber) {
      props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_BUILD_NUMBER, buildNumber)
    } else {
      props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_BUILD_NUMBER, System.currentTimeMillis() + "")
    }
    String buildName = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_BUILD_NAME, project)
    if (buildName) {
      buildName = buildName.replace(' ', '-')
      props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_BUILD_NAME, buildName)
    } else {
      Project rootProject = project.getRootProject();
      String defaultBuildName = rootProject.getName().replace(' ', '-')
      props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_BUILD_NAME, defaultBuildName)
    }
    String buildParentNumber = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER, project)
    if (buildParentNumber) props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_PARENT_BUILD_NUMBER, buildParentNumber)
    String buildParentName = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME, project)
    if (buildParentName) props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_PARENT_BUILD_NAME, buildParentName)
    String vcsRevision = ArtifactoryPluginUtils.getProperty(BuildInfoProperties.PROP_VCS_REVISION, project)
    if (vcsRevision) props.put(BuildInfoConfigProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + BuildInfoProperties.PROP_VCS_REVISION, vcsRevision)
    Map properties = project.getProperties()
    Set<String> keys = properties.keySet();
    for (String key: keys) {
      if (key != null) {
        Object value = properties.get(key)
        if (value != null) {
          value = value.toString().replace(" ", "-")
          props.put(key, value)
        }
      }
    }
    return DeploymentUrlUtils.getDeploymentUrl(uploadUrl, props)
  }

  private void configureBuildInfoTask(Project project) {
    if (project.tasks.findByName("buildInfo")) {
      return
    }
    BuildInfoRecorderTask buildInfo = project.getTasks().add("buildInfo", BuildInfoRecorderTask.class)
    buildInfo.dependsOn({
      (project.getTasks().withType(Jar)).all
    })

    def archivesConfiguration = project.getConfigurations().findByName(Dependency.DEFAULT_CONFIGURATION)
    if (archivesConfiguration != null) {
      buildInfo.setConfiguration(archivesConfiguration)
    }
    buildInfo.setDescription("Generates build info from build artifacts");
  }
}
