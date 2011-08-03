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
import org.jfrog.build.client.ArtifactoryClientConfiguration.ResolverHandler

/**
 * @author Tomer Cohen
 */
class ResolverConfig {

    private ResolverHandler resolver;
    private Repository repository;

    ResolverConfig(ArtifactoryPluginConvention conv) {
        resolver = conv.clientConfig.resolver
        repository = new Repository()
        repository.metaClass.propertyMissing = conv.propsResolver
    }

    def methodMissing(String name, args) {
        //println "1: missing method $name"
        Method[] methods = resolver.getClass().getMethods()
        Method method = methods.find {it.name.matches(name)}
        // TODO: [by fsi] Why only one parameter?
        method.invoke(resolver, args[0])
    }

    def propertyMissing(String name, value) {
        resolver[name] = value
    }

    def config(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    def repository(Closure closure) {
        //Initialize the defaults and configure the repo
        repository.setMaven(true)
        ConfigureUtil.configure(closure, repository)
    }

    public class Repository {

        def setUsername(String username) {
            ResolverConfig.this.resolver.setUsername(username)
        }

        def setPassword(String password) {
            ResolverConfig.this.resolver.setPassword(password)
        }

        def setIvyLayout(String ivyLayout) {
            ResolverConfig.this.resolver.setIvyPattern(ivyLayout)
            ResolverConfig.this.resolver.setIvyRepositoryDefined(true)
        }

        def setArtifactLayout(String artifactLayout) {
            ResolverConfig.this.resolver.setIvyArtifactPattern(artifactLayout)
            ResolverConfig.this.resolver.setIvyRepositoryDefined(true)
        }

        def setMavenCompatible(boolean mavenCompatible) {
            ResolverConfig.this.resolver.setM2Compatible(mavenCompatible)
        }

        def setRepoKey(String repoKey) {
            ResolverConfig.this.resolver.setRepoKey(repoKey)
        }

        def setMaven(Boolean maven) {
            ResolverConfig.this.resolver.setMaven(maven)
        }

        def ivy(Closure closure) {
            ConfigureUtil.configure(closure, this)
        }
    }
}


