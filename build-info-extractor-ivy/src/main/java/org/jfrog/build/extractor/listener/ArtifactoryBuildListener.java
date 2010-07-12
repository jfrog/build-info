package org.jfrog.build.extractor.listener;

import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.task.ArtifactoryPublishTask;
import org.jfrog.build.extractor.trigger.IvyBuildInfoTrigger;

import java.io.File;


/**
 * A listener which listens to the {@link Ant} builds, and is invoking different events during the build of {@code Ant}
 * itself! This is not to be confused with {@code Ivy} {@link Trigger} which is called during Ivy related events
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildListener extends BuildListenerAdapter {

    @Override
    public void buildStarted(BuildEvent event) {
        BuildContext ctx = new BuildContext();
        PropertyHelper ph = PropertyHelper.getPropertyHelper(event.getProject());
        ph.setUserProperty(BuildContext.CONTEXT_NAME, ctx);
        super.buildStarted(event);
    }

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
        project.log("Build finished triggered", Project.MSG_INFO);
        Object task = project.getTargets().get(ArtifactoryPublishTask.PUBLISH_ARTIFACT_TASK_NAME);
        if (task != null && task instanceof ArtifactoryPublishTask) {
            ArtifactoryPublishTask publishTask = (ArtifactoryPublishTask) task;
            publishTask.doExecute();
            IvyContext context = IvyContext.getContext();
            IvySettings ivySettings = context.getSettings();
            project.log("Reading build-info", Project.MSG_INFO);
            File buildInfoFile = new File(ivySettings.getBaseDir(), "build-info.json");
            /*       try {
                Build build = BuildInfoExtractorUtils.jsonStringToBuildInfo(FileUtils.readFileToString(buildInfoFile));
                ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient("http://localhost:8080");
                client.sendBuildInfo(build);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/
        }
    }
}
