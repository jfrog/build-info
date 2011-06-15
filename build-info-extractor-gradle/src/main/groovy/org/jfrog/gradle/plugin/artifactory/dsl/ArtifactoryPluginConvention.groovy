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

import com.google.common.collect.Lists
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
    final List<Closure> taskDefaultClosures
    final Closure propsResolver

    ArtifactoryPluginConvention(Project project) {
        this.project = project
        clientConfig = new ArtifactoryClientConfiguration(new GradleClientLogger(project.getLogger()))
        taskDefaultClosures = Lists.newArrayList()
        propsResolver = {String name ->
            project.logger.debug "Resolving property '${name}''"
            def val = project.properties[name]
            project.logger.debug "Property '${name}' resolved to '${project.properties[name]}'"
            val
        }
        ArtifactoryPluginConvention.metaClass.propertyMissing = propsResolver
    }

    def artifactory(Closure closure) {
        closure.delegate = this
        closure()
        project.logger.debug("Artifactory Plugin configured")
    }

    def setContextUrl(String contextUrl) {
        clientConfig.setContextUrl(contextUrl)
    }

    def publish(Closure closure) {
        PublisherConfig.metaClass.propertyMissing = propsResolver
        new PublisherConfig(this).config(closure)
    }

    def resolve(Closure closure) {
        ResolverConfig.metaClass.propertyMissing = propsResolver
        new ResolverConfig(this).config(closure)
    }

    def buildInfo(Closure closure) {
        ArtifactoryClientConfiguration.BuildInfoHandler.metaClass.propertyMissing = propsResolver
        ConfigureUtil.configure(closure, clientConfig.info)
    }

    def proxy(Closure closure) {
        ArtifactoryClientConfiguration.ProxyHandler.metaClass.propertyMissing = propsResolver
        ConfigureUtil.configure(closure, clientConfig.proxy)
    }
}

