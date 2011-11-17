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

import java.lang.reflect.Method
import org.gradle.util.ConfigureUtil
import org.jfrog.build.client.ArtifactoryClientConfiguration.PublisherHandler

/**
 * @author Tomer Cohen
 */
class PublisherConfig {

    private final PublisherHandler publisher;
    private final Repository repository;
    Closure taskDefaultClosure

    PublisherConfig(ArtifactoryPluginConvention conv) {
        publisher = conv.clientConfig.publisher
        repository = new Repository()
        repository.metaClass.propertyMissing = conv.propsResolver
    }

    def methodMissing(String name, args) {
        //println "1: missing method $name"
        Method[] methods = publisher.getClass().getMethods()
        def method = methods.find {it.name.matches(name)}
        method.invoke(publisher, args[0])
    }

    def propertyMissing(String name, value) {
        publisher[name] = value
    }

    def defaults(Closure closure) {
        //Add for later evaluation by the task iteslf after all projects evaluated
        taskDefaultClosure = closure
    }

    def config(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    def setContextUrl(String contextUrl) {
        publisher.setContextUrl(contextUrl)
    }

    def setPublishPom(boolean publishPom) {
        publisher.setMaven(publishPom)
    }

    def setPublishIvy(boolean publishIvy) {
        publisher.setIvy(publishIvy)
    }

    def repository(Closure closure) {
        ConfigureUtil.configure(closure, repository)
    }

    public class Repository {

        def setUsername(String username) {
            PublisherConfig.this.publisher.setUsername(username)
        }

        def setPassword(String password) {
            PublisherConfig.this.publisher.setPassword(password)
        }

        def setIvyLayout(String ivyLayout) {
            PublisherConfig.this.publisher.setIvyPattern(ivyLayout)
        }

        def setArtifactLayout(String artifactLayout) {
            PublisherConfig.this.publisher.setIvyArtifactPattern(artifactLayout)
        }

        def setRepoKey(String repoKey) {
            PublisherConfig.this.publisher.setRepoKey(repoKey)
        }

        def setMavenCompatible(boolean mavenCompatible) {
            PublisherConfig.this.publisher.setM2Compatible(mavenCompatible)
        }

        def ivy(Closure closure) {
            ConfigureUtil.configure(closure, this)
        }
    }
}
