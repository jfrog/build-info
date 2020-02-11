package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import java.util.Set;

public class ProjectUtils {
    /**
     * Finds the ArtifactoryTask in the project if it has executed.
     */
    static ArtifactoryTask getBuildInfoTask(Project project) {
        Set<Task> tasks = project.getTasksByName(ArtifactoryTask.ARTIFACTORY_PUBLISH_TASK_NAME, false);
        if (tasks.isEmpty()) {
            return null;
        }
        ArtifactoryTask artifactoryTask = (ArtifactoryTask)tasks.iterator().next();
        return artifactoryTask.getState().getDidWork() ? artifactoryTask : null;
    }
}
