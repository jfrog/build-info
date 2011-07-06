package org.jfrog.build

import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.gradle.BuildListener
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jfrog.build.client.ClientConfigurationFields
import org.jfrog.build.client.ClientProperties
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.extractor.BuildInfoRecorderTask
import org.jfrog.gradle.plugin.artifactory.extractor.GradlePluginUtils
import spock.lang.Specification
import static org.jfrog.build.client.ClientProperties.PROP_CONTEXT_URL
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_PROPS_FILE

/**
 * @author freds
 * @author Yoav Landman
 */
public class ArtifactoryPluginTest extends Specification {

    def nothingApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')

        artifactoryPlugin.apply(project)

        expect:
        project.buildscript.repositories.resolvers.isEmpty()
        project.repositories.resolvers.isEmpty()
        project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME) != null
    }

    def resolverApplyPlugin() {
        Project project = ProjectBuilder.builder().build()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        String rootUrl = 'http://localhost:8081/artifactory/'
        project.setProperty(PROP_CONTEXT_URL, rootUrl)
        project.setProperty(ClientProperties.PROP_RESOLVE_PREFIX + ClientConfigurationFields.REPO_KEY, 'repo')
        String expectedName = rootUrl + 'repo'

        artifactoryPlugin.apply(project)
        projectEvaluated(project)

        // TODO: Test the buildSrc project issue
        List libsResolvers = project.repositories.resolvers
        expect:
        libsResolvers.size() == 1
        libsResolvers.get(0) instanceof org.apache.ivy.plugins.resolver.IBiblioResolver
        libsResolvers.get(0).name == expectedName
        ((IBiblioResolver) libsResolvers.get(0)).root == expectedName + '/'
        project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME) != null
    }

    def buildInfoJavaPlugin() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        expect:
        project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME) != null
    }

    def buildInfoTaskConfiguration() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'true')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        BuildInfoRecorderTask buildInfoTask = project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME)
        projectEvaluated(project)
        expect:
        buildInfoTask.configuration != null
    }

    def buildInfoTaskDependsOn() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'false')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        Task buildInfoTask = project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME)
        projectEvaluated(project)
        expect:
        buildInfoTask.dependsOn != null
        !buildInfoTask.dependsOn.isEmpty()
        buildInfoTask.dependsOn.size() == 1
    }

    def populateConfigurationFromDsl() {
        // Make sure no system props are set
        def propFileEnv = System.getenv(PROP_PROPS_FILE)
        if (propFileEnv != null && propFileEnv.length() > 0) {
            throw new RuntimeException("Cannot run test if environment variable "+PROP_PROPS_FILE+ " is set")
        }
        if (System.getProperty(PROP_PROPS_FILE)) {
            System.clearProperty(PROP_PROPS_FILE)
        }
        URL resource = getClass().getResource('/org/jfrog/build/build.gradle')
        def projDir = new File(resource.toURI()).getParentFile()

        Project project = ProjectBuilder.builder().withProjectDir(projDir).build()
        project.setProperty('testUserName', 'user1')
        project.setProperty('testPassword', 'p33p')
        project.setProperty('ppom', false)

        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        //project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'true')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        BuildInfoRecorderTask buildInfoTask = project.tasks.findByName(GradlePluginUtils.BUILD_INFO_TASK_NAME)
        def clientConfig = GradlePluginUtils.getArtifactoryConvention(project).getClientConfig()
        project.evaluate()
        projectEvaluated(project)

        expect:
        buildInfoTask.configuration != null
        '[ext]user1' == clientConfig.publisher.username
        'p33p' == clientConfig.publisher.password
        //Cannot call clientConfig.publisher.isMaven() since it is only assigned at task execution
        !buildInfoTask.getPublishPom()
    }

    private def projectEvaluated(Project project) {
        BuildListener next = project.getGradle().listenerManager.allListeners.iterator().next()
        next.projectsEvaluated(project.getGradle())
    }
}


/*GradleLauncher.injectCustomFactory(
        new DefaultGradleLauncherFactory(LoggingServiceRegistry.newEmbeddableLogging()))
StartParameter parameter = new StartParameter();
parameter.projectProperties = ['username': 'user1', 'passwd': 'p33p', 'ppom': 'false',
        "${ClientConfigurationFields.REPO_KEY}": '',
        "${ClientProperties.PROP_PUBLISH_PREFIX}${ClientConfigurationFields.IVY}": 'true']
parameter.setProjectDir(projDir)
GradleLauncher gradleLauncher = GradleLauncher.newInstance(parameter);
def project
gradleLauncher.addListener(new BuildAdapter() {
    public void buildFinished(BuildResult result) {
        project = result.gradle.rootProject;
    }
})
gradleLauncher.run()*/