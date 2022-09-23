package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.ResolverHandler

import java.lang.reflect.Method

/**
 * @author Tomer Cohen
 */
class ResolverConfig {

    private final Project project;
    private final ResolverHandler resolver;
    private final Repository repository;

    ResolverConfig(ArtifactoryPluginConvention conv) {
        resolver = conv.clientConfig.resolver
        repository = new Repository()
        project = conv.project
    }

    def methodMissing(String name, args) {
        //println "1: missing method $name"
        Method[] methods = resolver.getClass().getMethods()
        Method method = methods.find {it.name.matches(name)}
        method.invoke(resolver, args[0])
    }

    def propertyMissing(String name) {
        project.property(name)
    }

    def propertyMissing(String name, value) {
        resolver[name] = value
    }

    def setContextUrl(def contextUrl) {
        resolver.setContextUrl(contextUrl?.toString())
    }

    def config(Closure closure) {
        config(ConfigureUtil.configureUsing(closure))
    }

    def config(Action<? extends ResolverConfig> configAction) {
        configAction.execute(this)
    }

    def repository(Closure closure) {
        repository(ConfigureUtil.configureUsing(closure))
    }

    def repository(Action<? extends Repository> repositoryAction) {
        //Initialize the defaults and configure the repo
        repository.setMaven(true)
        repositoryAction.execute(repository)
    }

    public class Repository {

        def setUsername(def username) {
            ResolverConfig.this.resolver.setUsername(username?.toString())
        }

        def setPassword(def password) {
            ResolverConfig.this.resolver.setPassword(password?.toString())
        }

        def setIvyLayout(def ivyLayout) {
            ResolverConfig.this.resolver.setIvyPattern(ivyLayout?.toString())
            if (ivyLayout) {
                ResolverConfig.this.resolver.setIvyRepositoryDefined(true)
            }
        }

        def setArtifactLayout(def artifactLayout) {
            ResolverConfig.this.resolver.setIvyArtifactPattern(artifactLayout?.toString())
            if (artifactLayout) {
                ResolverConfig.this.resolver.setIvyRepositoryDefined(true)
            }
        }

        def setAllowInsecureProtocol(boolean allow) {
            ResolverConfig.this.resolver.setAllowInsecureProtocol(allow);
        }

        def setMavenCompatible(boolean mavenCompatible) {
            ResolverConfig.this.resolver.setM2Compatible(mavenCompatible)
        }

        def setRepoKey(def repoKey) {
            ResolverConfig.this.resolver.setRepoKey(repoKey?.toString())
        }

        def setMaven(Boolean maven) {
            ResolverConfig.this.resolver.setMaven(maven)
        }

        def ivy(Closure closure) {
            ivy(ConfigureUtil.configureUsing(closure))
        }

        def ivy(Action<? extends Repository> ivyAction) {
            ivyAction.execute(this)
        }
    }
}


