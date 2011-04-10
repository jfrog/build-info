package org.jfrog.dsl

import com.google.common.collect.Lists
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.ConfigureUtil
import org.jfrog.build.ArtifactoryPluginUtils
import org.jfrog.build.client.ArtifactoryClientConfiguration

/**
 * @author Tomer Cohen
 */
class ArtifactoryPluginConvention {
    private Logger logger
    ArtifactoryClientConfiguration configuration
    final List<Closure> projectDefaultClosures = Lists.newArrayList()

    ArtifactoryPluginConvention(Project project) {
        this.logger = project.logger
        configuration = ArtifactoryPluginUtils.getArtifactoryClientConfiguration(project)
    }

    def artifactory(Closure closure) {
        closure.delegate = this
        closure()
        logger.debug("Artifactory Plugin configured")
    }

    def projectDefaults(Closure closure) {
        projectDefaultClosures.add(closure)
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

