package org.jfrog.gradle.plugin.artifactory.extractor.listener

import org.apache.commons.lang3.StringUtils
import org.gradle.BuildAdapter
import org.gradle.StartParameter
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.invocation.Gradle
import org.gradle.api.publish.Publication
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.extractor.GradleArtifactoryClientConfigUpdater
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.jfrog.gradle.plugin.artifactory.task.helper.TaskHelperPublications
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.annotation.Nullable
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
public class ProjectsEvaluatedBuildListener extends BuildAdapter implements ProjectEvaluationListener {
    private static final Logger log = LoggerFactory.getLogger(ProjectsEvaluatedBuildListener.class)
    private final Set<Task> artifactoryTasks = Collections.newSetFromMap(new ConcurrentHashMap<Task, Boolean>());

    @Override
    void beforeEvaluate(Project project) {
    }

    /**
     * This method is invoked after evaluation of every project
     */
    @Override
    void afterEvaluate(Project project, ProjectState state) {
        Set<Task> tasks = project.getTasksByName(ARTIFACTORY_PUBLISH_TASK_NAME, false)
        StartParameter startParameter = project.getGradle().getStartParameter();
        tasks.each { ArtifactoryTask artifactoryTask ->
            artifactoryTasks.add(artifactoryTask)
            artifactoryTask.finalizeByDeployTask(project)
            if (startParameter.isConfigureOnDemand()) {
                evaluate(artifactoryTask)
            }
        }
    }

    private void addMavenJavaPublication(PublishingExtension publishingExtension, Project project) {
        if (publishingExtension.getPublications().findByName(TaskHelperPublications.MAVEN_JAVA) != null) {
            // mavenJava publication already exists
            return
        }
        project.plugins.withType(MavenPublishPlugin) { MavenPublishPlugin publishingPlugin ->
            publishingExtension.with {
                publications {
                    mavenJava(MavenPublication) {
                        from project.components.java
                    }
                }
            }
        }
    }

    private void addMavenJavaPlatformPublication(PublishingExtension publishingExtension, Project project) {
        if (publishingExtension.getPublications().findByName(TaskHelperPublications.MAVEN_JAVA_PLATFORM) != null) {
            // mavenJavaPlatform publication already exists
            return
        }
        project.plugins.withType(MavenPublishPlugin) { MavenPublishPlugin publishingPlugin ->
            publishingExtension.with {
                publications {
                    mavenJavaPlatform(MavenPublication) {
                        from project.components.javaPlatform
                    }
                }
            }
        }
    }

    private void addMavenWebPublication(PublishingExtension publishingExtension, Project project) {
        if (publishingExtension.getPublications().findByName(TaskHelperPublications.MAVEN_WEB) != null) {
            // mavenWeb publication already exists
            return
        }
        project.plugins.withType(MavenPublishPlugin) { MavenPublishPlugin publishingPlugin ->
            publishingExtension.with {
                publications {
                    mavenWeb(MavenPublication) {
                        from project.components.web
                    }
                }
            }
        }
    }

    private void addIvyJavaPublication(PublishingExtension publishingExtension, Project project) {
        if (publishingExtension.getPublications().findByName(TaskHelperPublications.IVY_JAVA) != null) {
            // ivyJava publication already exists
            return
        }
        project.plugins.withType(IvyPublishPlugin) { IvyPublishPlugin publishingPlugin ->
            publishingExtension.with {
                publications {
                    ivyJava(IvyPublication) {
                        from project.components.java
                    }
                }
            }
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
            if (artifactoryTask.isCiServerBuild()) {
                PublishingExtension publishingExtension = (PublishingExtension) artifactoryTask.project.extensions.findByName("publishing")
                String publicationsNames = clientConfig.publisher.getPublications()
                if (publishingExtension != null && StringUtils.isNotBlank(publicationsNames)) {
                    addPublications(artifactoryTask, publishingExtension, publicationsNames)
                } else if (projectHasOneOfComponents(artifactoryTask.project, "java", "javaPlatform")) {
                    addDefaultPublicationsOrConfigurations(artifactoryTask, publishingExtension);
                }
            }
            artifactoryTask.projectEvaluated()
        }
    }

    /**
     * Add publications to artifactory task.
     * @param artifactoryTask - The artifactory task
     * @param publishingExtension - The publishing extension: 'maven-publish' or 'ivy-publish'
     * @param publicationsNames - Publications names separated by commas
     */
    private static void addPublications(ArtifactoryTask artifactoryTask, PublishingExtension publishingExtension, String publicationsNames) {
        if (StringUtils.isEmpty(publicationsNames)) {
            return
        }

        PublicationContainer container = publishingExtension.getPublications()
        Collection<String> ciPublications = publicationsNames.split(",")

        // If ALL_PUBLICATIONS parameter was provided, add all publications and return.
        if (ciPublications.contains(TaskHelperPublications.ALL_PUBLICATIONS)) {
            for (publication in container) {
                artifactoryTask.publications(publication)
            }
            return
        }
        // Add specified publications.
        for (publicationName in ciPublications) {
            Publication publication = container.findByName(publicationName);
            artifactoryTask.publications(publication)
        }
    }

    /**
     * Return true if at least one of the input components exists in the project.
     * If the 'java' component exists - 'java' or 'java-library' plugins are applied.
     * If the 'javaPlatform' component exists - 'java-platform' plugin is applied.
     * @param project - The Gradle project of the task
     * @return true if at least one of the input components exists in the project.
     */
    private static boolean projectHasOneOfComponents(Project project, String... componentNames) {
        for (componentName in componentNames) {
            if (project.components.findByName(componentName) != null) {
                return true
            }
        }
        return false
    }

    /**
     * When running the gradle build from a CI server or JFrog CLI with the 'Project Uses the Artifactory Plugin' option
     * set to false, the init script generated by the CI server sets the 'addPublishDefaultTasks' boolean to true.
     * With 'maven-publish' and 'ivy-publish' plugins - Add the default mavenJava, mavenJavaPlatform, mavenWeb and ivyJava publications if needed.
     * With 'maven' plugin (deprecated) - Add the default 'archives' gradle configuration.
     * @param artifactoryTask - The Artifactory task
     * @param publishingExtension - The publishing extension or null if the project uses configurations
     */
    private void addDefaultPublicationsOrConfigurations(ArtifactoryTask artifactoryTask, @Nullable PublishingExtension publishingExtension) {
        if (publishingExtension != null) {
            Project project = artifactoryTask.project;
            // Add mavenWeb publication if war task exists and enabled
            Task warTask = project.tasks.findByName("war");
            if (warTask != null && warTask.enabled) {
                addMavenWebPublication(publishingExtension, project)
            }

            // Add mavenJava and ivyJava publications if jar task doesn't exist, or exists and enabled
            Task jarTask = project.tasks.findByName("jar");
            if (jarTask == null || jarTask.enabled) {
                if (projectHasOneOfComponents(project, "java")) {
                    addMavenJavaPublication(publishingExtension, project)
                    addIvyJavaPublication(publishingExtension, project)
                }
                if (projectHasOneOfComponents(project, "javaPlatform")) {
                    addMavenJavaPlatformPublication(publishingExtension, project)
                }
            }

            // Add publications to Artifactory task
            artifactoryTask.addDefaultPublications()
        } else {
            artifactoryTask.addDefaultArchiveConfiguration()
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
        IvyArtifactRepository ivyRepo = project.repositories.ivy {
            name = 'artifactory-ivy-resolver'
            url = resolverConf.urlWithMatrixParams(pUrl)
            if (StringUtils.isNotBlank(resolverConf.username) && StringUtils.isNotBlank(resolverConf.password)) {
                credentials {
                    username = resolverConf.username
                    password = resolverConf.password
                }
            }
        }
        if (ivyRepo.metaClass.respondsTo(ivyRepo, 'patternLayout')) {
            // Gradle 5 an above
            ivyRepo.patternLayout {
                artifact resolverConf.getIvyArtifactPattern()
                ivy resolverConf.getIvyPattern()
            }
        } else {
            // Gradle 4
            ivyRepo.layout 'pattern', {
                artifact resolverConf.getIvyArtifactPattern()
                ivy resolverConf.getIvyPattern()
            }
        }
        return ivyRepo
    }

    /**
     * This method is invoked after all projects are evaluated
     */
    @Override
    void projectsEvaluated(Gradle gradle) {
        Set<Task> tasks = gradle.rootProject.getTasksByName(ARTIFACTORY_PUBLISH_TASK_NAME, false)
        artifactoryTasks.addAll(tasks)
        artifactoryTasks.each { ArtifactoryTask artifactoryTask ->
            if (!artifactoryTask.isEvaluated()) {
                evaluate(artifactoryTask)
                artifactoryTask.finalizeByDeployTask(artifactoryTask.getProject())
            }
        }
    }
}
