package org.jfrog.gradle.plugin.artifactory

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.jfrog.gradle.plugin.artifactory.task.DeployTask
import org.jfrog.gradle.plugin.artifactory.task.DistributeBuildTask
import org.jfrog.gradle.plugin.artifactory.task.ExtractModuleTask

import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME
import static org.jfrog.gradle.plugin.artifactory.task.DistributeBuildTask.DISTRIBUTE_TASK_NAME
import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.DEPLOY_TASK_NAME
import static org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask.EXTRACT_MODULE_TASK_NAME

/**
 * @author Lior Hasson
 */
class ArtifactoryPlugin extends ArtifactoryPluginBase {
    @Override
    protected ArtifactoryPluginConvention createArtifactoryPluginConvention(Project project) {
        return new ArtifactoryPluginConvention(project)
    }

    @Override
    protected TaskProvider<ArtifactoryTask> createArtifactoryPublishTask(Project project, Action<ArtifactoryTask> configurationAction) {
        return project.getTasks().register(ARTIFACTORY_PUBLISH_TASK_NAME, ArtifactoryTask.class, configurationAction)
    }

    @Override
    protected TaskProvider<DistributeBuildTask> createArtifactoryDistributeBuildTask(Project project, Action<DistributeBuildTask> configurationAction) {
        return project.getTasks().register(DISTRIBUTE_TASK_NAME, DistributeBuildTask.class, configurationAction)
    }

    @Override
    protected TaskProvider<DeployTask> createArtifactoryDeployTask(Project project, Action<DeployTask> configurationAction) {
        return project.getTasks().register(DEPLOY_TASK_NAME, DeployTask.class, configurationAction)
    }

    @Override
    protected TaskProvider<ExtractModuleTask> createExtractModuleTask(Project project, Action<ExtractModuleTask> configurationAction) {
        return project.getTasks().register(EXTRACT_MODULE_TASK_NAME, ExtractModuleTask.class, configurationAction)
    }
}
