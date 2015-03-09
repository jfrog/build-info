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
        ConfigureUtil.configure(closure, this)
    }

    def propertyMissing(String name, value) {
        info."$name" = value
    }
}