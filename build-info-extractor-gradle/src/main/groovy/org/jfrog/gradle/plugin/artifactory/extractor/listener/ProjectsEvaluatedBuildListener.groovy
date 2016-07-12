package org.jfrog.gradle.plugin.artifactory.extractor.listener

import org.apache.commons.lang.StringUtils
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask.BUILD_INFO_TASK_NAME

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
        if (project.getPlugins().hasPlugin(ArtifactoryPlugin.class)) {
            ArtifactoryClientConfiguration configuration =
                    ArtifactoryPluginUtil.getArtifactoryConvention(project.gradle.rootProject).getClientConfig()
            //Fill-in the client config for the global.
            GradleArtifactoryClientConfigUpdater.update(configuration, project.gradle.rootProject)
            defineResolvers(project, configuration.resolver)
            BuildInfoBaseTask bit = project.getTasks().getByName(BUILD_INFO_TASK_NAME)
            bit.projectsEvaluated()
        }
    }

    @Override
    void afterEvaluate(Project project, ProjectState state) {
        if (project.getPlugins().hasPlugin(ArtifactoryPlugin.class)) {
            if (project == project.getRootProject()) {
                BuildInfoBaseTask bit = project.getTasks().getByName(BUILD_INFO_TASK_NAME)
                bit.projectsEvaluated()
            }
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
