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

import java.lang.reflect.Method
import org.gradle.util.ConfigureUtil
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.PublisherHandler

/**
 * @author Tomer Cohen
 */
class PublisherConfig {
    private final Project project
    private final PublisherHandler publisher;
    private final Repository repository;
    Closure defaultsClosure

    PublisherConfig(ArtifactoryPluginConvention conv) {
        publisher = conv.clientConfig.publisher
        project = conv.project
        repository = new Repository()
    }

    def methodMissing(String name, args) {
        //println "1: missing method $name"
        Method[] methods = publisher.getClass().getMethods()
        def method = methods.find {it.name.matches(name)}
        try {
            method.invoke(publisher, args[0])
        }
        catch (any){
            printf "$args[0]"
        }
    }

    def propertyMissing(String name) {
        project.property(name)
    }

    def propertyMissing(String name, value) {
        publisher[name] = value
    }

    def defaults(Closure closure) {
        //Add for later evaluation by the task itself after all projects evaluated
        defaultsClosure = closure
    }

    def config(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    def setContextUrl(def contextUrl) {
        publisher.setContextUrl(contextUrl?.toString())
    }

    def setPublishPom(boolean publishPom) {
        publisher.setMaven(publishPom)
    }

    def setPublishIvy(boolean publishIvy) {
        publisher.setIvy(publishIvy)
    }

    def repository(Closure closure) {
        ConfigureUtil.configure(closure, new DoubleDelegateWrapper(project, repository))
    }

    public class Repository {

        def setUsername(def username) {
            PublisherConfig.this.publisher.setUsername(username?.toString())
        }

        def setPassword(def password) {
            PublisherConfig.this.publisher.setPassword(password?.toString())
        }

        def setIvyLayout(def ivyLayout) {
            PublisherConfig.this.publisher.setIvy(true)
            PublisherConfig.this.publisher.setIvyPattern(ivyLayout?.toString())
        }

        def setArtifactLayout(def artifactLayout) {
            PublisherConfig.this.publisher.setIvyArtifactPattern(artifactLayout?.toString())
        }

        def setRepoKey(def repoKey) {
            PublisherConfig.this.publisher.setRepoKey(repoKey?.toString())
        }

        def setMavenCompatible(boolean mavenCompatible) {
            PublisherConfig.this.publisher.setM2Compatible(mavenCompatible)
        }

        def ivy(Closure closure) {
            ConfigureUtil.configure(closure, this)
        }
    }
}