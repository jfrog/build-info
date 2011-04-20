/*
 * Copyright (C) 2010 JFrog Ltd.
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
    new PublisherConfig(configuration.publisher).config(closure)
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

