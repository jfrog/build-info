package org.jfrog.build.extractor.listener;

import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;

import java.io.IOException;
import java.util.Date;
import java.util.Set;


/**
 * A listener which listens to the {@link Ant} builds, and is invoking different events during the build of {@code Ant}
 * itself! This is not to be confused with {@code Ivy} {@link Trigger} which is called during Ivy related events
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildListener extends BuildListenerAdapter {
    private BuildContext ctx = new BuildContext();
    private long started;

    @Override
    public void buildStarted(BuildEvent event) {
        IvyContext context = IvyContext.getContext();
        context.set(BuildContext.CONTEXT_NAME, ctx);
        started = System.currentTimeMillis();
        super.buildStarted(event);
    }

    /**
     * Called when the build has ended, this is the time where we will assemble the build-info object that its
     * information was collected by the {@link org.jfrog.build.extractor.trigger.ArtifactoryBuildInfoTrigger} it will
     * serialize the build-info object into a senadble JSON object to be used by the {@link ArtifactoryBuildInfoClient}
     *
     * @param event The build event.
     */
    @Override
    public void buildFinished(BuildEvent event) {
        Project project = event.getProject();
        project.log("Build finished triggered", Project.MSG_INFO);
        BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
        Set<DeployDetails> deployDetails = ctx.getDeployDetails();
        BuildInfoBuilder builder = new BuildInfoBuilder(project.getName()).modules(ctx.getModules())
                .number("0").durationMillis(System.currentTimeMillis() - started).startedDate(new Date(started))
                .buildAgent(new BuildAgent("Ivy", Ivy.getIvyVersion())).agent(new Agent("Ivy", Ivy.getIvyVersion()));
        Build build = builder.build();
        try {
            ArtifactoryBuildInfoClient client =
                    new ArtifactoryBuildInfoClient("http://localhost:8080/artifactory/", "admin", "password");
            deployArtifacts(client, deployDetails);
            deployBuildInfo(client, build);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deployArtifacts(ArtifactoryBuildInfoClient client, Set<DeployDetails> deployDetails)
            throws IOException {
        for (DeployDetails deployDetail : deployDetails) {
            client.deployArtifact(deployDetail);
        }
    }

    private void deployBuildInfo(ArtifactoryBuildInfoClient client, Build build) throws IOException {
        client.sendBuildInfo(build);
    }
}
