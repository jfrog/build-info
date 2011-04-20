package org.jfrog.build

import org.gradle.util.HelperUtil

import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.gradle.BuildListener
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.jfrog.build.client.ClientConfigurationFields
import org.jfrog.build.client.ClientProperties
import org.jfrog.build.extractor.gradle.BuildInfoRecorderTask
import spock.lang.Specification
import static org.jfrog.build.client.ClientProperties.PROP_CONTEXT_URL

/**
 * @author freds
 */
public class ArtifactoryPluginTest extends Specification {

  def nothingApplyPlugin() {
    Project project = HelperUtil.createRootProject()
    ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

    // Disable resolving
    project.setProperty(ClientConfigurationFields.REPO_KEY, '')

    artifactoryPlugin.apply(project)

    expect:
    project.buildscript.repositories.resolvers.isEmpty()
    project.repositories.resolvers.isEmpty()
    project.tasks.findByName('buildInfo') != null
  }

  def resolverApplyPlugin() {
    Project project = HelperUtil.createRootProject()
    ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

    String rootUrl = 'http://localhost:8081/artifactory/'
    project.setProperty(PROP_CONTEXT_URL, rootUrl)
    project.setProperty(ClientProperties.PROP_RESOLVE_PREFIX + ClientConfigurationFields.REPO_KEY, 'repo')
    project.setProperty(ClientProperties.PROP_RESOLVE_PREFIX + ClientConfigurationFields.ENABLED, 'true')
    String expectedName = rootUrl + 'repo'

    artifactoryPlugin.apply(project)

    // TODO: Test the buildSrc project issue
    List libsResolvers = project.repositories.resolvers
    expect:
    libsResolvers.size() == 1
    libsResolvers.get(0) instanceof org.apache.ivy.plugins.resolver.IBiblioResolver
    libsResolvers.get(0).name == expectedName
    ((IBiblioResolver) libsResolvers.get(0)).root == expectedName + '/'
    project.tasks.findByName('buildInfo') != null
  }

  def buildInfoJavaPlugin() {
    Project project = HelperUtil.createRootProject()
    JavaPlugin javaPlugin = new JavaPlugin()
    ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

    // Disable resolving
    project.setProperty(ClientConfigurationFields.REPO_KEY, '')
    javaPlugin.apply(project)
    artifactoryPlugin.apply(project)

    expect:
    project.tasks.findByName('buildInfo') != null
  }

  def buildInfoTaskConfiguration() {
    Project project = HelperUtil.createRootProject()
    JavaPlugin javaPlugin = new JavaPlugin()
    ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

    // Disable resolving
    project.setProperty(ClientConfigurationFields.REPO_KEY, '')
    project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'true')
    project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
    javaPlugin.apply(project)
    artifactoryPlugin.apply(project)

    BuildInfoRecorderTask buildInfoTask = project.tasks.findByName('buildInfo')
    evaluateSettings(project)
    expect:
    buildInfoTask.configuration != null
  }

  def buildInfoTaskDependsOn() {
    Project project = HelperUtil.createRootProject()
    JavaPlugin javaPlugin = new JavaPlugin()
    ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

    // Disable resolving
    project.setProperty(ClientConfigurationFields.REPO_KEY, '')
    project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'false')
    project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
    javaPlugin.apply(project)
    artifactoryPlugin.apply(project)

    Task buildInfoTask = project.tasks.findByName('buildInfo')
    evaluateSettings(project)
    expect:
    buildInfoTask.dependsOn != null
    !buildInfoTask.dependsOn.isEmpty()
    buildInfoTask.dependsOn.size() == 1
  }

  private def evaluateSettings(Project project) {
    BuildListener next = project.getGradle().listenerManager.allListeners.iterator().next()
    next.projectsEvaluated(project.getGradle())
  }
}
