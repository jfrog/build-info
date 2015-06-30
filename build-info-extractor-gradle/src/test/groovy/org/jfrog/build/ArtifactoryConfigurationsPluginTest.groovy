package org.jfrog.build

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpec
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginBase
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_PROPS_FILE
import static org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields.*
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_PUBLISH_PREFIX
import static org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask.BUILD_INFO_TASK_NAME

/**
 * @author freds
 * @author Yoav Landman
 */
public class ArtifactoryConfigurationsPluginTest extends PluginTestBase {

    @Override
    ArtifactoryPluginBase createPlugin() {
        return new ArtifactoryPlugin()
    }

    def buildInfoTaskConfiguration() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.ext.set(REPO_KEY, '')
        project.getGradle().getStartParameter().projectProperties['BOOLEAN_START_PARAM'] = true
        project.ext.set(PROP_PUBLISH_PREFIX + IVY, 'true')
        project.ext.set(PROP_PUBLISH_PREFIX + MAVEN, 'false')
        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        ArtifactoryTask buildInfoTask = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        projectEvaluated(project)
        expect:
        buildInfoTask.ivyDescriptor != null
    }

    def buildInfoTaskDependsOn() {
        Project project = ProjectBuilder.builder().build()
        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        // Disable resolving
        project.ext.set(REPO_KEY, '')
        project.ext.set(PROP_PUBLISH_PREFIX + IVY, 'false')
        project.ext.set(PROP_PUBLISH_PREFIX + MAVEN, 'false')
        project.ext.set(PROP_PUBLISH_PREFIX + PUBLISH_ARTIFACTS, 'false')
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

    def populateConfigurationFromDsl() {
        // Make sure no system props are set
        def propFileEnv = System.getenv(PROP_PROPS_FILE)
        if (propFileEnv != null && propFileEnv.length() > 0) {
            throw new RuntimeException("Cannot run test if environment variable " + PROP_PROPS_FILE + " is set")
        }
        if (System.getProperty(PROP_PROPS_FILE)) {
            System.clearProperty(PROP_PROPS_FILE)
        }
        URL resource = getClass().getResource('/org/jfrog/build/confPluginDslTest/build.gradle')
        def projDir = new File(resource.toURI()).getParentFile()

        Project project = ProjectBuilder.builder().withProjectDir(projDir).build()
        project.ext.set('testUserName', 'user1')
        project.ext.set('testPassword', 'p33p')
        project.ext.set('ppom', false)

        //Set artifact specs
        project.ext.set(PROP_PUBLISH_PREFIX + ARTIFACT_SPECS,
                'archives com.jfrog:*:*:doc@* key1: val1, key2: val2\n' +
                        'archives com.jfrog:*:*:src@* key3: val 3')

        JavaPlugin javaPlugin = new JavaPlugin()
        ArtifactoryPlugin artifactoryPlugin = new ArtifactoryPlugin()

        javaPlugin.apply(project)
        artifactoryPlugin.apply(project)

        ArtifactoryTask buildInfoTask = project.tasks.findByName(BUILD_INFO_TASK_NAME)
        def clientConfig = ArtifactoryPluginUtil.getArtifactoryConvention(project).getClientConfig()
        project.evaluate()
        projectEvaluated(project)

        expect:
        !buildInfoTask.publishConfigurations.isEmpty()
        '[ext]user1' == clientConfig.publisher.username
        'p33p' == clientConfig.publisher.password
        !clientConfig.resolver.maven
        //Cannot call clientConfig.publisher.isMaven() since it is only assigned at task execution
        !buildInfoTask.getPublishPom()
        buildInfoTask.artifactSpecs[0].group == 'com.jfrog'
        buildInfoTask.artifactSpecs[0].classifier == 'doc'
        buildInfoTask.artifactSpecs[1].group == 'com.jfrog'
        buildInfoTask.artifactSpecs[1].classifier == 'src'
        buildInfoTask.artifactSpecs[1].properties['key3'] == 'val 3'
        buildInfoTask.artifactSpecs[2].group == 'org.jfrog'
        buildInfoTask.artifactSpecs[2].classifier == ArtifactSpec.WILDCARD
    }
}
