package org.jfrog.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jfrog.build.client.ArtifactSpec
import org.jfrog.build.client.ClientConfigurationFields
import org.jfrog.build.client.ClientProperties
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginBase
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPublicationsPlugin
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoConfigurationsTask
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoPublicationsTask

import static BuildInfoConfigurationsTask.BUILD_INFO_TASK_NAME
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_PROPS_FILE

/**
 * @author freds
 * @author Yoav Landman
 */
public class ArtifactoryPublicationsPluginTest extends PluginTestBase {

    ArtifactoryPluginBase createPlugin() {
        new ArtifactoryPublicationsPlugin()
    }

    def populatePublicationFromDsl() {
        // Make sure no system props are set
        def propFileEnv = System.getenv(PROP_PROPS_FILE)
        if (propFileEnv != null && propFileEnv.length() > 0) {
            throw new RuntimeException("Cannot run test if environment variable " + PROP_PROPS_FILE + " is set")
        }
        if (System.getProperty(PROP_PROPS_FILE)) {
            System.clearProperty(PROP_PROPS_FILE)
        }
        URL resource = getClass().getResource('/org/jfrog/build/publishPluginDslTest/build.gradle')
        def projDir = new File(resource.toURI()).getParentFile()

        Project project = ProjectBuilder.builder().withProjectDir(projDir).build()
        project.ext.set('testUserName', 'user1')
        project.ext.set('testPassword', 'p33p')
        project.ext.set('ppom', false)

        //Set artifact specs
        project.ext.set(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.ARTIFACT_SPECS,
                'archives com.jfrog:*:*:doc@* key1: val1, key2: val2\n' +
                        'archives com.jfrog:*:*:src@* key3: val 3')

        project.plugins.apply(JavaPlugin)
        project.plugins.apply(IvyPublishPlugin)
        project.plugins.apply(MavenPublishPlugin)
        project.plugins.apply(ArtifactoryPublicationsPlugin)

        BuildInfoPublicationsTask buildInfoTask = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        def clientConfig = ArtifactoryPluginUtil.getArtifactoryConvention(project).getClientConfig()
        project.evaluate()
        projectEvaluated(project)

        expect:
        buildInfoTask.hasPublications()
        '[ext]user1' == clientConfig.publisher.username
        'p33p' == clientConfig.publisher.password
        !clientConfig.resolver.maven
        buildInfoTask.ivyPublications.size() == 1
        buildInfoTask.mavenPublications.size() == 1
        buildInfoTask.filesToPublish.size() == 1
        buildInfoTask.ivyPublications.iterator().next().name == 'ivyJava'
        buildInfoTask.mavenPublications.iterator().next().name == 'mavenJava'
        buildInfoTask.artifactSpecs[0].group == 'com.jfrog'
        buildInfoTask.artifactSpecs[0].classifier == 'doc'
        buildInfoTask.artifactSpecs[1].group == 'com.jfrog'
        buildInfoTask.artifactSpecs[1].classifier == 'src'
        buildInfoTask.artifactSpecs[1].properties['key3'] == 'val 3'
        buildInfoTask.artifactSpecs[2].group == 'org.jfrog'
        buildInfoTask.artifactSpecs[2].classifier == ArtifactSpec.WILDCARD
    }

/*
    def buildInfoTaskConfiguration() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'true')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        BuildInfoBaseTask buildInfoTask = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        projectEvaluated(project)
        expect:
        buildInfoTask.ivyDescriptor != null
    }

    def buildInfoTaskDependsOn() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPluginBase artifactoryPlugin = createPlugin()

        // Disable resolving
        project.setProperty(ClientConfigurationFields.REPO_KEY, '')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.IVY, 'false')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.MAVEN, 'false')
        project.setProperty(ClientProperties.PROP_PUBLISH_PREFIX + ClientConfigurationFields.PUBLISH_ARTIFACTS, 'false')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        Task buildInfoTask = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        projectEvaluated(project)
        expect:
        buildInfoTask.dependsOn != null
        !buildInfoTask.dependsOn.isEmpty()
        // depends on the archives configuration, and the archives.artifacts file collection
        buildInfoTask.dependsOn.size() == 2
    }

*/
}
