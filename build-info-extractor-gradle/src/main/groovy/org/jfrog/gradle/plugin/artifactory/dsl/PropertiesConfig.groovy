package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownConfigurationException
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpec
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpecs

/**
 * @author Yoav Landman
 */
class PropertiesConfig {
    final Project project
    final ArtifactSpecs artifactSpecs = new ArtifactSpecs()

    PropertiesConfig(org.gradle.api.Project project) {
        this.project = project
    }

    def methodMissing(String name, args) {
        def artifactSpecNotation
        def props
        switch (args.length) {
            case 2:
                //Verify the configuration exists
                if (ArtifactSpec.CONFIG_ALL != name) {
                    try {
                        project.getConfigurations().getByName(name)
                    } catch (UnknownConfigurationException e) {
                        project.logger.info("Artifactory plugin: configuration '$name' not found in project '${project.path}'")
                    }
                }
                artifactSpecNotation = args[1]
                props = args[0].each {it.value = it.value.toString()}
                break
            default:
                throw new GradleException("Invalid artifact properties spec: $name, $args.\nExpected: configName artifactSpec, key1:val1, key2:val2")
        }
        def spec = ArtifactSpec.builder().artifactNotation(artifactSpecNotation).configuration(name).properties(props).build()
        artifactSpecs.add(spec)
    }
}