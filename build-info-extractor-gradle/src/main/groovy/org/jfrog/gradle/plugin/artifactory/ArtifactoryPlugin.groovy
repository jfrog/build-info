package org.jfrog.gradle.plugin.artifactory

import org.gradle.api.Project
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask
import org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask

import static org.jfrog.gradle.plugin.artifactory.task.BuildInfoBaseTask.BUILD_INFO_TASK_NAME

/**
 * @author Lior Hasson  
 */
class ArtifactoryPlugin extends ArtifactoryPluginBase{
    @Override
    protected ArtifactoryPluginConvention createArtifactoryPluginConvention(Project project) {
        return new ArtifactoryPluginConvention(project)
    }

    @Override
    protected BuildInfoBaseTask createArtifactoryPublishTask(Project project) {
        def result = project.getTasks().create(BUILD_INFO_TASK_NAME, ArtifactoryTask.class)
        result.setDescription('''Deploys artifacts + generated build-info metadata to Artifactory,
                                 using project configurations.''')
        return result
    }
}
