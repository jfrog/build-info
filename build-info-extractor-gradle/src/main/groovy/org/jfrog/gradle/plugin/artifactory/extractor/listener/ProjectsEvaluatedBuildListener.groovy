package org.jfrog.gradle.plugin.artifactory.extractor.listener

import org.apache.commons.lang.StringUtils
import org.gradle.BuildAdapter
import org.gradle.api.Project
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.invocation.Gradle
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.gradle.api.ProjectEvaluationListener
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME

/**
 * @author Lior Hasson
 *
 *
 * A class that represents Build event listener to prepare data for "artifactoryPublish" Task
 * via the "projectEvaluated" build event.
 * Main actions:
 * 1) Grabbing user and system properties
 * 2) Overriding gradle resolution repositories (Maven/Ivy)
 * 3) Prepare artifacts for deployment
 */
public class ProjectsEvaluatedBuildListener implements ProjectEvaluationListener {
    private static final Logger log = LoggerFactory.getLogger(ProjectsEvaluatedBuildListener.class)

    @Override
    void beforeEvaluate(Project project) {
    }

    @Override
    void afterEvaluate(Project project, ProjectState state) {
        Set<Task> tasks = project.getTasksByName(ARTIFACTORY_PUBLISH_TASK_NAME, false)
        tasks.each { ArtifactoryTask artifactoryTask ->
            evaluate(artifactoryTask)
        }
    }

    private void evaluate(ArtifactoryTask artifactoryTask) {
        log.debug("evaluating buildBaseTask {}", artifactoryTask)
        ArtifactoryPluginConvention convention =
                ArtifactoryPluginUtil.getArtifactoryConvention(artifactoryTask.project)

        if (convention != null) {
            ArtifactoryClientConfiguration clientConfig = convention.getClientConfig()
            // Fill-in the client config for the global, then adjust children project
            GradleArtifactoryClientConfigUpdater.update(clientConfig, artifactoryTask.project)
            ArtifactoryClientConfiguration.ResolverHandler resolver =
                    ArtifactoryPluginUtil.getResolverHandler(artifactoryTask.project)
            if (resolver != null) {
                defineResolvers(artifactoryTask.project, resolver)
            }
            artifactoryTask.projectEvaluated()
        }
    }

    private void defineResolvers(Project project, ArtifactoryClientConfiguration.ResolverHandler resolverConf) {
        String url = resolverConf.getUrl()
        if (StringUtils.isNotBlank(url)) {
            log.debug("Artifactory URL: $url")
            // Add artifactory url to the list of repositories
            createMavenRepo(project, url, resolverConf)
            createIvyRepo(project, url, resolverConf)
        } else {
            log.debug("No repository resolution defined for ${project.path}")
        }
    }

    private def createMavenRepo(Project project, String pUrl, ArtifactoryClientConfiguration.ResolverHandler resolverConf) {
        return project.repositories.maven {
            name = 'artifactory-maven-resolver'
            url = resolverConf.urlWithMatrixParams(pUrl)
            if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                credentials {
                    username = resolverConf.username
                    password = resolverConf.password
                }
            }
        }
    }

    private def createIvyRepo(Project project, String pUrl, ArtifactoryClientConfiguration.ResolverHandler resolverConf) {
        return project.repositories.ivy {
            name = 'artifactory-ivy-resolver'
            url = resolverConf.urlWithMatrixParams(pUrl)
            layout 'pattern', {
                artifact resolverConf.getIvyArtifactPattern()
                ivy resolverConf.getIvyPattern()
            }
            if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                credentials {
                    username = resolverConf.username
                    password = resolverConf.password
                }
            }
        }
    }
}
