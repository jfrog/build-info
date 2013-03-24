package org.jfrog.build

import org.gradle.BuildListener
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jfrog.build.client.ClientConfigurationFields
import org.jfrog.build.client.ClientProperties
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginBase
import spock.lang.Specification

import static org.spockframework.util.Assert.notNull
import static org.spockframework.util.Assert.that
import static org.spockframework.util.Assert.that

import static org.jfrog.gradle.plugin.artifactory.task.BuildInfoConfigurationsTask.BUILD_INFO_TASK_NAME
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_PROPS_FILE
import static org.jfrog.build.client.ClientProperties.PROP_CONTEXT_URL
import static org.spockframework.util.Assert.notNull
import static org.spockframework.util.Assert.that

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
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')

        artifactoryPlugin.apply(project)

        expect:
        that(project.buildscript.repositories.resolvers.isEmpty())
        that(project.repositories.resolvers.isEmpty())
        notNull(project.tasks.findByName(BUILD_INFO_TASK_NAME))
    }

    def resolverApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        String rootUrl = 'http://localhost:8081/artifactory/'
        project.setProperty(PROP_CONTEXT_URL, rootUrl)
        project.setProperty(ClientProperties.PROP_RESOLVE_PREFIX + ClientConfigurationFields.REPO_KEY, 'repo')
        String expectedName = 'artifactory-maven-resolver'

        artifactoryPlugin.apply(project)
        projectEvaluated(project)

        // TODO: Test the buildSrc project issue
        List libsResolvers = project.repositories.resolvers
        expect:
        that libsResolvers.size() == 1
        that libsResolvers.get(0).name == expectedName
        that libsResolvers.get(0).root == rootUrl + 'repo/'
        notNull project.tasks.findByName(BUILD_INFO_TASK_NAME)
    }

    def buildInfoJavaPlugin() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        expect:
        project.tasks.findByName(BUILD_INFO_TASK_NAME) != null
    }
}
