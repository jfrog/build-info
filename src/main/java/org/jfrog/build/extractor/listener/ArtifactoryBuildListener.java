package org.jfrog.build.extractor.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.util.IvyBuildInfoLog;

import java.io.IOException;
import java.util.Date;
import java.util.Properties;
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
    private static boolean isDidDeploy;

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
        if (event.getException() != null) {
            return;
        }
        if (!isDidDeploy) {
            Project project = event.getProject();
            project.log("Build finished triggered", Project.MSG_INFO);
            BuildContext ctx = (BuildContext) IvyContext.getContext().get(BuildContext.CONTEXT_NAME);
            Set<DeployDetails> deployDetails = ctx.getDeployDetails();
            BuildInfoBuilder builder = new BuildInfoBuilder(project.getName()).modules(ctx.getModules())
                    .number("0").durationMillis(System.currentTimeMillis() - started).startedDate(new Date(started))
                    .buildAgent(new BuildAgent("Ivy", Ivy.getIvyVersion()))
                    .agent(new Agent("Ivy", Ivy.getIvyVersion()));
            // This is here for backwards compatibility.
            builder.type(BuildType.IVY);
            Properties envProps = new Properties();
            envProps.putAll(System.getenv());
            Properties mergedProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(envProps);
            String buildAgentName = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_AGENT_NAME);
            String buildAgentVersion = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_AGENT_VERSION);
            if (StringUtils.isNotBlank(buildAgentName) && StringUtils.isNotBlank(buildAgentVersion)) {
                builder.buildAgent(new BuildAgent(buildAgentName, buildAgentVersion));
            }
            String buildName = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_NAME);
            if (StringUtils.isNotBlank(buildName)) {
                builder.name(buildName);
            }
            String buildNumber = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER);
            if (StringUtils.isNotBlank(buildNumber)) {
                builder.number(buildNumber);
            }
            String buildUrl = mergedProps.getProperty("BUILD_URL");
            if (StringUtils.isNotBlank(buildUrl)) {
                builder.url(buildUrl);
            }
            String principal = mergedProps.getProperty(BuildInfoProperties.PROP_PRINCIPAL);
            if (StringUtils.isNotBlank(principal)) {
                builder.principal(principal);
            }
            String parentBuildName = mergedProps.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME);
            if (StringUtils.isNotBlank(parentBuildName)) {
                builder.parentName(parentBuildName);
            }
            String parentBuildNumber = mergedProps.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER);
            if (StringUtils.isNotBlank(parentBuildNumber)) {
                builder.parentNumber(parentBuildNumber);
            }
            Properties props = BuildInfoExtractorUtils.getEnvProperties(mergedProps);
            Properties propsFromSys = BuildInfoExtractorUtils
                    .filterDynamicProperties(System.getProperties(), BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);
            props.putAll(propsFromSys);
            builder.properties(props);
            Build build = builder.build();
            String contextUrl = mergedProps.getProperty(ClientProperties.PROP_CONTEXT_URL);
            String username = mergedProps.getProperty(ClientProperties.PROP_PUBLISH_USERNAME);
            String password = mergedProps.getProperty(ClientProperties.PROP_PUBLISH_PASSWORD);
            try {
                ArtifactoryBuildInfoClient client =
                        new ArtifactoryBuildInfoClient(contextUrl, username, password, new IvyBuildInfoLog(project));
                boolean isDeployArtifacts =
                        Boolean.parseBoolean(mergedProps.getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT));
                if (isDeployArtifacts) {
                    deployArtifacts(client, deployDetails);
                }
                boolean isDeployBuildInfo =
                        Boolean.parseBoolean(mergedProps.getProperty(ClientProperties.PROP_PUBLISH_BUILD_INFO));
                if (isDeployBuildInfo) {
                    deployBuildInfo(client, build);
                }
                isDidDeploy = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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
