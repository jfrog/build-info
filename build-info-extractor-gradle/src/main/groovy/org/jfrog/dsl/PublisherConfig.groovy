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

import java.lang.reflect.Method
import org.gradle.util.ConfigureUtil
import org.jfrog.build.client.ArtifactoryClientConfiguration.PublisherHandler

/**
 * @author Tomer Cohen
 */
class PublisherConfig {

    private PublisherHandler handler;
    private Repository repository;

    PublisherConfig(PublisherHandler handler) {
        this.handler = handler
        this.repository = new Repository()
    }

    def methodMissing(String name, args) {
        //println "1: missing method $name"
        Method[] methods = handler.getClass().getMethods()
        def method = methods.find {it.name.matches(name)}
        method.invoke(handler, args[0])
    }

    def propertyMissing(String name, value) {
        handler[name] = value
    }


    def config(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    def setPublishPom(boolean publishPom) {
        handler.setMaven(publishPom)
    }

    def setPublishIvy(boolean publishIvy) {
        handler.setIvy(publishIvy)
    }

    def repository(Closure closure) {
        ConfigureUtil.configure(closure, repository)
    }

    public class Repository {

        def setUsername(String username) {
            handler.setUserName(username)
        }

        def setPassword(String password) {
            handler.setPassword(password)
        }

        def setIvyLayout(String ivyLayout) {
            handler.setIvyPattern(ivyLayout)
        }

        def setArtifactLayout(String artifactLayout) {
            handler.setIvyArtifactPattern(artifactLayout)
        }

        def setRepoKey(String repoKey) {
            handler.setRepoKey(repoKey)
        }

        def setMavenCompatible(boolean mavenCompatible) {
            handler.setM2Compatible(mavenCompatible)
        }

        def ivy(Closure closure) {
            ConfigureUtil.configure(closure, this)
        }
    }
}


