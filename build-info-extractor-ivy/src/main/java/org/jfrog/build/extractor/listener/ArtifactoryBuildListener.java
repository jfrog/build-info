package org.jfrog.build.extractor.listener;

import org.apache.commons.io.FileUtils;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.task.ArtifactoryPublishTask;
import org.jfrog.build.extractor.trigger.IvyBuildInfoTrigger;

import java.io.File;
import java.io.IOException;


/**
 * A listener which listens to the {@link Ant} builds, and is invoking different events during the build of {@code Ant}
 * itself! This is not to be confused with {@code Ivy} {@link Trigger} which is called during Ivy related events
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildListener extends BuildListenerAdapter {

    /**
     * Called when the build has ended, this is the time where we will read the build-info object that was assembled by
     * the {@link IvyBuildInfoTrigger}, it will parse the file into a senadble build-info object to be used by the
     * {@link ArtifactoryBuildInfoClient}
     *
     * @param event The build event.
     */
    @Override
    public void buildFinished(BuildEvent event) {
        Project project = event.getProject();
        ArtifactoryPublishTask publishTask =
                (ArtifactoryPublishTask) project.getTargets().get(ArtifactoryPublishTask.PUBLISH_ARTIFACT_TASK_NAME);
        publishTask.doExecute();
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        project.log("Reading build-info", Project.MSG_INFO);
        File buildInfoFile = new File(ivySettings.getBaseDir(), "build-info.json");
        try {
            Build build = BuildInfoExtractorUtils.jsonStringToBuildInfo(FileUtils.readFileToString(buildInfoFile));
            ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient("http://localhost:8080");
            client.sendBuildInfo(build);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
