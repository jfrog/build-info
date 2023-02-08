package org.jfrog.gradle.plugin.artifactory

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.extractor.ModuleInfoFileProducer
import org.jfrog.gradle.plugin.artifactory.extractor.listener.ArtifactoryDependencyResolutionListener
import org.jfrog.gradle.plugin.artifactory.extractor.listener.ProjectsEvaluatedBuildListener
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.jfrog.gradle.plugin.artifactory.task.DeployTask
import org.jfrog.gradle.plugin.artifactory.task.DistributeBuildTask
import org.jfrog.gradle.plugin.artifactory.task.ExtractModuleTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.*
import static org.jfrog.gradle.plugin.artifactory.task.DistributeBuildTask.DISTRIBUTE_TASK_NAME

abstract class ArtifactoryPluginBase implements Plugin<Project> {
    private static final Logger log = LoggerFactory.getLogger(ArtifactoryPluginBase.class)
    public static final String PUBLISH_TASK_GROUP = "publishing"
    private ArtifactoryDependencyResolutionListener artifactoryDependencyResolutionListener = new ArtifactoryDependencyResolutionListener()

    void apply(Project project) {
        if ("buildSrc".equals(project.name)) {
            log.debug("Artifactory Plugin disabled for ${project.path}")
            return
        }
        // Add an Artifactory plugin convention to all the project modules
        ArtifactoryPluginConvention conv = getArtifactoryPluginConvention(project)
        // Then add the artifactory publish task
        ArtifactoryTask artifactoryTaskProvider = addArtifactoryPublishTask(project)
        // Add the module info producer task
        addModuleInfoTask(project, artifactoryTaskProvider)

        if (isRootProject(project)) {
            addDeployTask(project)
            addDistributeBuildTask(project)

            // Add a DependencyResolutionListener, to populate the dependency hierarchy map.
            project.getGradle().addListener(artifactoryDependencyResolutionListener)
        } else {
            // Makes sure the plugin is applied in the root project
            project.rootProject.getPluginManager().apply(ArtifactoryPlugin.class)
        }

        if (!conv.clientConfig.info.buildStarted) {
            conv.clientConfig.info.setBuildStarted(System.currentTimeMillis())
        }
        log.debug("Using Artifactory Plugin for ${project.path}")

        project.gradle.addProjectEvaluationListener(new ProjectsEvaluatedBuildListener())
    }

    protected abstract TaskProvider<ArtifactoryTask> createArtifactoryPublishTask(Project project, Action<ArtifactoryTask> configurationAction)
    protected abstract TaskProvider<DistributeBuildTask> createArtifactoryDistributeBuildTask(Project project, Action<DistributeBuildTask> configurationAction)
    protected abstract ArtifactoryPluginConvention createArtifactoryPluginConvention(Project project)
    protected abstract TaskProvider<DeployTask> createArtifactoryDeployTask(Project project, Action<DeployTask> configurationAction);
    protected abstract TaskProvider<ExtractModuleTask> createExtractModuleTask(Project project, Action<ExtractModuleTask> configurationAction);

    ArtifactoryDependencyResolutionListener getArtifactoryDependencyResolutionListener() {
        return artifactoryDependencyResolutionListener
    }

    /**
     *  Set the plugin convention closure object
     *  artifactory {
     *      ...
     *  }
     */
    private ArtifactoryPluginConvention getArtifactoryPluginConvention(Project project) {
        if (project.convention.plugins.artifactory == null) {
            project.convention.plugins.artifactory = createArtifactoryPluginConvention(project)
        }
        return project.convention.plugins.artifactory
    }

    private static boolean isRootProject(Project project) {
        project.equals(project.getRootProject())
    }

    /**
     * Add the "artifactoryPublish" gradle task (under "publishing" task group)
     */
    private TaskProvider<ArtifactoryTask> addArtifactoryPublishTask(Project project) {
        try {
            return project.tasks.named(ARTIFACTORY_PUBLISH_TASK_NAME, ArtifactoryTask)
        } catch (UnknownTaskException ignored) {
            log.debug("Configuring ${ARTIFACTORY_PUBLISH_TASK_NAME} task for project ${project.path}: is root? ${isRootProject(project)}")
            return createArtifactoryPublishTask(project) {
                setDescription('''Adds artifacts and generates build-info to be later deployed to Artifactory.''')
                setGroup(PUBLISH_TASK_GROUP)
            }
        }
    }

    /**
     * Add the "artifactoryDistribute" gradle task (under "publishing" task group)
     */
    private void addDistributeBuildTask(Project project) {
        try {
            // will throw if task not found
            project.tasks.named(DISTRIBUTE_TASK_NAME)
        } catch (UnknownTaskException ignored) {
            log.debug("Configuring ${DISTRIBUTE_TASK_NAME} task for project ${project.path}: is root? ${isRootProject(project)}")
            createArtifactoryDistributeBuildTask(project) {
                setDescription('''Distributes build artifacts to Bintray.''')
                setGroup(PUBLISH_TASK_GROUP)
            }
        }
    }

    private void addModuleInfoTask(Project project, TaskProvider<ArtifactoryTask> artifactoryTaskProvider) {

        try {
            project.tasks.named(EXTRACT_MODULE_TASK_NAME)
        } catch (UnknownTaskException ignored) {
            log.debug("Configuring extractModuleInfo task for project ${project.path}")
            createExtractModuleTask(project) { extractModuleTask ->
                setDescription('''Extracts module info to an intermediate file''')
                outputs.upToDateWhen { false }
                moduleFile.set(project.layout.buildDirectory.file("moduleInfo.json"))
                mustRunAfter(extractModuleTask.project.tasks.withType(ArtifactoryTask.class))

                extractModuleTask.project.rootProject.tasks.withType(DeployTask).configureEach { deployTask ->
                    deployTask.registerModuleInfoProducer(new DefaultModuleInfoFileProducer(artifactoryTaskProvider.get(), extractModuleTask))
                }
            }
        }
    }

    private void addDeployTask(Project project) {
        try {
            project.tasks.named(DEPLOY_TASK_NAME)
        } catch (UnknownTaskException ignored) {
            log.debug("Configuring deployTask task for project ${project.path}")
            createArtifactoryDeployTask(project) {
                setDescription('''Deploys artifacts and build-info to Artifactory.''')
                setGroup(PUBLISH_TASK_GROUP)
            }
        }
    }

    private static class DefaultModuleInfoFileProducer implements ModuleInfoFileProducer {
        private final ArtifactoryTask artifactoryTask
        private final ExtractModuleTask extractModuleTask

        DefaultModuleInfoFileProducer(ArtifactoryTask artifactoryTask, ExtractModuleTask extractModuleTask) {
            this.artifactoryTask = artifactoryTask
            this.extractModuleTask = extractModuleTask
        }

        @Override
        boolean hasModules() {
            if (artifactoryTask != null && artifactoryTask.project.getState().getExecuted()) {
                return artifactoryTask.hasModules();
            }
            return false;
        }

        @Override
        FileCollection getModuleInfoFiles() {
            return extractModuleTask.outputs.files
        }
    }
}
