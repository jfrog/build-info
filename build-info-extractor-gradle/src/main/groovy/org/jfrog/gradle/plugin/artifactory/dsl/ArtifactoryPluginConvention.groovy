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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger

/**
 * @author Tomer Cohen
 */
class ArtifactoryPluginConvention {
    final Project project
    final ArtifactoryClientConfiguration clientConfig
    def PublisherConfig publisherConfig
    def DistributerConfig distributerConfig
    def boolean conventionSet = false

    ArtifactoryPluginConvention(Project project) {
        this.project = project
        clientConfig = new ArtifactoryClientConfiguration(new GradleClientLogger(project.getLogger()))
    }

    def artifactory(Closure closure) {
        artifactory(ConfigureUtil.configureUsing(closure))
    }

    def artifactory(Action<? extends ArtifactoryPluginConvention> artifactoryAction) {
        artifactoryAction.execute(this)
        project.logger.debug("Artifactory plugin: configured")
        conventionSet = true
    }

    def propertyMissing(String name) {
        project.property(name)
    }

    def setContextUrl(def contextUrl) {
        clientConfig.publisher.setContextUrl(contextUrl?.toString())
        clientConfig.resolver.setContextUrl(contextUrl?.toString())
    }

    def distribute(Closure closure) {
        distribute(ConfigureUtil.configureUsing(closure))
    }

    def distribute(Action<? extends DistributerConfig> distributeAction) {
        distributerConfig = new DistributerConfig(this)
        distributeAction.execute(distributerConfig)
    }

    def publish(Closure closure) {
        publish(ConfigureUtil.configureUsing(closure))
    }

    def publish(Action<? extends PublisherConfig> publishAction) {
        publisherConfig = new PublisherConfig(this)
        publishAction.execute(publisherConfig)
    }

    def resolve(Closure closure) {
        resolve(ConfigureUtil.configureUsing(closure))
    }

    def resolve(Action<? extends ResolverConfig> resolveAction) {
        resolveAction.execute(new ResolverConfig(this))
    }

    def buildInfo(Closure closure) {
        ConfigureUtil.configure(closure, new DoubleDelegateWrapper(project, clientConfig.info))
    }

    def buildInfo(Action<? extends ArtifactoryClientConfiguration.BuildInfoHandler> infoAction) {
        infoAction.execute(clientConfig.info)
    }

    def proxy(Closure closure) {
        ConfigureUtil.configure(closure, new DoubleDelegateWrapper(project, clientConfig.proxy))
    }

    def proxy(Action<? extends ArtifactoryClientConfiguration.ProxyHandler> proxyAction) {
        proxyAction.execute(clientConfig.proxy)
    }

    def parent(Closure closure) {
        parent(ConfigureUtil.configureUsing(closure))
    }

    def parent(Action<? extends ParentConfig> parentAction) {
        parentAction.execute(new ParentConfig(this))
    }
}

