package org.jfrog.dsl

import org.gradle.api.logging.Logger
import org.gradle.util.ConfigureUtil
import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.build.extractor.logger.GradleClientLogger
import org.jfrog.build.ArtifactoryPluginUtils
import org.gradle.api.Project

/**
 * @author Tomer Cohen
 */
class ArtifactoryPluginConvention {
    private Logger logger
    ArtifactoryClientConfiguration configuration

    ArtifactoryPluginConvention(Project project) {
        this.logger = project.logger
        configuration = ArtifactoryPluginUtils.getArtifactoryClientConfiguration(project)
    }

    def artifactory(Closure closure) {
        closure.delegate = this
        closure()
        logger.debug("Artfiactory Plugin configured")
    }

    def publish(Closure closure) {
        ConfigureUtil.configure(closure, configuration.publisher)
    }

    def resolve(Closure closure) {
        ConfigureUtil.configure(closure, configuration.resolver)
    }

    def buildInfo(Closure closure) {
        ConfigureUtil.configure(closure, configuration.info)
    }

    def proxy(Closure closure) {
        ConfigureUtil.configure(closure, configuration.proxy)
    }
}

