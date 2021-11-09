package org.jfrog.gradle.plugin.artifactory.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.PublisherHandler
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask

import java.lang.reflect.Method

/**
 * @author Tomer Cohen
 */
class PublisherConfig {
    private final Project project
    private final PublisherHandler publisher;
    private final Repository repository;
    Action<ArtifactoryTask> defaultsAction

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
        defaults(ConfigureUtil.configureUsing(closure))
    }

    def defaults(Action<ArtifactoryTask> action) {
        //Add for later evaluation by the task itself after all projects evaluated
        defaultsAction = action
    }

    def config(Closure closure) {
        config(ConfigureUtil.configureUsing(closure))
    }

    def config(Action<? extends PublisherConfig> configAction) {
        configAction.execute(this)
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

    def repository(Action<? extends Repository> repositoryAction) {
        repositoryAction.execute(repository)
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
            ivy(ConfigureUtil.configureUsing(closure))
        }

        def ivy(Action<? extends Repository> ivyAction) {
            ivyAction.execute(this)
        }
    }
}