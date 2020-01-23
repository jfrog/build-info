package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskState;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import java.lang.reflect.Method;
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
        if (ProjectUtils.taskDidWork(artifactoryTask)) {
            return artifactoryTask;
        }
        return null;
    }

    /**
     * Determines if the task actually did any work.
     * This methods wraps Gradle's task.getState().getDidWork().
     *
     * @param task The ArtifactoryTask
     * @return true if the task actually did any work.
     */
    static boolean taskDidWork(ArtifactoryTask task) {
        try {
            return task.getState().getDidWork();
        } catch (NoSuchMethodError error) {
            // Compatibility with older versions of Gradle:
            try {
                Method m = task.getClass().getMethod("getState");
                TaskState state = (TaskState) m.invoke(task);
                return state.getDidWork();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}