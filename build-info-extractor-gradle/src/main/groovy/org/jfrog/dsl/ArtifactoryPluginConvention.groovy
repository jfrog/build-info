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

package org.jfrog.dsl

import com.google.common.collect.Lists
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.ConfigureUtil
import org.jfrog.build.client.ArtifactoryClientConfiguration
import org.jfrog.build.extractor.gradle.logger.GradleClientLogger

/**
 * @author Tomer Cohen
 */
class ArtifactoryPluginConvention {
    private Logger logger
    final ArtifactoryClientConfiguration configuration
    final List<Closure> taskDefaultClosures

    ArtifactoryPluginConvention(Project project) {
        this.logger = project.logger
        configuration = new ArtifactoryClientConfiguration(new GradleClientLogger(project.getLogger()))
        taskDefaultClosures = Lists.newArrayList()
    }

    List<Closure> getTaskDefaults() {
        return taskDefaultClosures
    }

    def artifactory(Closure closure) {
        closure.delegate = this
        closure()
        logger.debug("Artifactory Plugin configured")
    }

    def setContextUrl(String contextUrl) {
        configuration.setContextUrl(contextUrl)
    }

    def publish(Closure closure) {
        new PublisherConfig(configuration.publisher, taskDefaultClosures).config(closure)
    }

    def resolve(Closure closure) {
        new ResolverConfig(configuration.resolver).config(closure)
    }

    def buildInfo(Closure closure) {
        ConfigureUtil.configure(closure, configuration.info)
    }

    def proxy(Closure closure) {
        ConfigureUtil.configure(closure, configuration.proxy)
    }
}

