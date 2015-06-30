package org.jfrog.build

import org.gradle.BuildListener
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields
import org.jfrog.build.extractor.clientConfiguration.ClientProperties
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginBase
import spock.lang.Specification

import static org.spockframework.util.Assert.notNull
import static org.spockframework.util.Assert.that
import static org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask.BUILD_INFO_TASK_NAME
import static ClientProperties.PROP_CONTEXT_URL

/**
 *
 * Date: 3/24/13
 * Time: 10:05 PM
 * @author freds
 */
abstract class PluginTestBase extends Specification {

    abstract ArtifactoryPluginBase createPlugin()

    def projectEvaluated(Project project) {
        BuildListener next = project.getGradle().listenerManager.allListeners.iterator().next()
        next.projectsEvaluated(project.getGradle())
    }

    def nothingApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        // Disable resolving
        project.ext.set(ClientConfigurationFields.REPO_KEY, '')

        artifactoryPlugin.apply(project)

        expect:
        that(project.buildscript.repositories.isEmpty())
        that(project.repositories.isEmpty())
        notNull(project.tasks.findByName(BUILD_INFO_TASK_NAME))
    }

    def resolverApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        String rootUrl = 'http://localhost:8081/artifactory/'
        project.ext.set(PROP_CONTEXT_URL, rootUrl)
        project.ext.set(ClientProperties.PROP_RESOLVE_PREFIX + ClientConfigurationFields.REPO_KEY, 'repo')
        String expectedName = 'artifactory-maven-resolver'

        artifactoryPlugin.apply(project)
        projectEvaluated(project)

        // TODO: Test the buildSrc project issue
        DefaultMavenArtifactRepository libsResolvers = project.repositories.getByName(expectedName)
        expect:

        that libsResolvers.name == expectedName
        that libsResolvers.url.toString() == rootUrl + 'repo'
        notNull project.tasks.findByName(BUILD_INFO_TASK_NAME)
    }

    def buildInfoJavaPlugin() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        // Disable resolving
        project.ext.set(ClientConfigurationFields.REPO_KEY, '')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        expect:
        project.tasks.findByName(BUILD_INFO_TASK_NAME) != null
    }
}
