package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Action
import org.gradle.util.ConfigureUtil
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration

/**
 * @author Noam Y. Tenne
 */

class ParentConfig {

    private final ArtifactoryClientConfiguration.BuildInfoHandler info

    ParentConfig(ArtifactoryPluginConvention conv) {
        info = conv.clientConfig.info
        info.incremental = Boolean.TRUE
    }

    def config(Closure closure) {
        config(ConfigureUtil.configureUsing(closure))
    }

    def config(Action<? extends ParentConfig> configAction) {
        configAction.execute(this)
    }

    def propertyMissing(String name, value) {
        info."$name" = value
    }
}
