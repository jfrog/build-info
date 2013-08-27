/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger

/**
 * @author Tomer Cohen
 */
class ArtifactoryPluginConvention {
    final Project project
    final ArtifactoryClientConfiguration clientConfig
    def PublisherConfig publisherConfig

    ArtifactoryPluginConvention(Project project) {
        this.project = project
        clientConfig = new ArtifactoryClientConfiguration(new GradleClientLogger(project.getLogger()))
    }

    def artifactory(Closure closure) {
        closure.delegate = this
        closure.call()
        project.logger.debug("Artifactory plugin: configured")
    }

    def propertyMissing(String name) {
        project.property(name)
    }

    def setContextUrl(def contextUrl) {
        clientConfig.publisher.setContextUrl(contextUrl?.toString())
        clientConfig.resolver.setContextUrl(contextUrl?.toString())
    }

    def publish(Closure closure) {
        publisherConfig = new PublisherConfig(this)
        publisherConfig.config(closure)
    }

    def resolve(Closure closure) {
        new ResolverConfig(this).config(closure)
    }

    def buildInfo(Closure closure) {
        ConfigureUtil.configure(closure, new DoubleDelegateWrapper(clientConfig.info))
    }

    def proxy(Closure closure) {
        ConfigureUtil.configure(closure, new DoubleDelegateWrapper(clientConfig.proxy))
    }
}

