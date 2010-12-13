package org.jfrog.build

import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.gradle.api.Project
import org.gradle.util.HelperUtil
import spock.lang.Specification
import static org.jfrog.build.client.ClientProperties.PROP_CONTEXT_URL
import static org.jfrog.build.client.ClientProperties.PROP_RESOLVE_REPOKEY
import org.gradle.api.plugins.JavaPlugin

/**
 * @author freds
 */
public class ArtifactoryPluginTest extends Specification {

    def nothingApplyPlugin() {
        Project project = HelperUtil.createRootProject()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(PROP_RESOLVE_REPOKEY, '')

        artifactoryPlugin.apply(project)

        expect:
        project.buildscript.repositories.resolvers.isEmpty()
        project.repositories.resolvers.isEmpty()
        project.tasks.findByName('buildInfo') == null
    }

    def resolverApplyPlugin() {
        Project project = HelperUtil.createRootProject()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        String rootUrl = 'http://localhost:8081/artifactory/'
        project.setProperty(PROP_CONTEXT_URL, rootUrl)
        project.setProperty(PROP_RESOLVE_REPOKEY, 'repo')
        String expectedName = rootUrl + 'repo'

        artifactoryPlugin.apply(project)

        // TODO: Test the buildSrc project issue
        List libsResolvers = project.repositories.resolvers
        expect:
        libsResolvers.size() == 1
        libsResolvers.get(0) instanceof org.apache.ivy.plugins.resolver.IBiblioResolver
        libsResolvers.get(0).name == expectedName
        ((IBiblioResolver) libsResolvers.get(0)).root == expectedName + '/'
        project.tasks.findByName('buildInfo') == null
    }

    def buildInfoJavaPlugin() {
        Project project = HelperUtil.createRootProject()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(PROP_RESOLVE_REPOKEY, '')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        expect:
        project.tasks.findByName('buildInfo') != null
    }
}
