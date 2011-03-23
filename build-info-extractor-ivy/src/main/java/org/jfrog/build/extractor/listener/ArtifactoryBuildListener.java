package org.jfrog.build.extractor.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.publish.StartArtifactPublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.core.resolve.ResolveEngine;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.LicenseControl;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.client.IncludeExcludePatterns;
import org.jfrog.build.client.PatternMatcher;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.trigger.ArtifactoryBuildInfoTrigger;
import org.jfrog.build.util.IvyBuildInfoLog;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PROP_PREFIX;


/**
 * A listener which listens to the {@link Ant} builds, and is invoking different events during the build of {@code Ant}
 * itself! This is not to be confused with {@code Ivy} {@link Trigger} which is called during Ivy related events
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildListener extends BuildListenerAdapter {
    private BuildContext ctx = new BuildContext();
    private static boolean isDidDeploy;
    private static final ArtifactoryBuildInfoTrigger DEPENDENCY_TRIGGER = new ArtifactoryBuildInfoTrigger();
    private static final ArtifactoryBuildInfoTrigger PUBLISH_TRIGGER = new ArtifactoryBuildInfoTrigger();

    public ArtifactoryBuildListener() {
        DEPENDENCY_TRIGGER.setEvent(EndResolveEvent.NAME);
        PUBLISH_TRIGGER.setEvent(StartArtifactPublishEvent.NAME);
    }

    @Override
    public void buildStarted(BuildEvent event) {
        IvyContext context = IvyContext.getContext();
        context.set(BuildContext.CONTEXT_NAME, ctx);
        ctx.setBuildStartTime(System.currentTimeMillis());
        super.buildStarted(event);
    }


    @Override
    public void taskStarted(BuildEvent event) {
        ResolveEngine engine = IvyAntSettings.getDefaultInstance(event.getTask()).
                getConfiguredIvyInstance(event.getTask()).getResolveEngine();
        EventManager engineEventManager = engine.getEventManager();
        engineEventManager.removeIvyListener(DEPENDENCY_TRIGGER);
        engineEventManager.addIvyListener(DEPENDENCY_TRIGGER, DEPENDENCY_TRIGGER.getEventFilter());
        IvyContext context = IvyContext.getContext();
        EventManager eventManager = context.getIvy().getEventManager();
        eventManager.removeIvyListener(PUBLISH_TRIGGER);
        eventManager.addIvyListener(PUBLISH_TRIGGER, PUBLISH_TRIGGER.getEventFilter());
        context.getIvy().bind();
        super.taskStarted(event);
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
                    .number("0").durationMillis(System.currentTimeMillis() - ctx.getBuildStartTime())
                    .startedDate(new Date(ctx.getBuildStartTime()))
                    .buildAgent(new BuildAgent("Ivy", Ivy.getIvyVersion()))
                    .agent(new Agent("Ivy", Ivy.getIvyVersion()));
            // This is here for backwards compatibility.
            builder.type(BuildType.IVY);
            Properties envProps = new Properties();
            envProps.putAll(System.getenv());
            Properties mergedProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(envProps);

            String agentName = mergedProps.getProperty(BuildInfoProperties.PROP_AGENT_NAME);
            String agentVersion = mergedProps.getProperty(BuildInfoProperties.PROP_AGENT_VERSION);
            if (StringUtils.isNotBlank(agentName) && StringUtils.isNotBlank(agentVersion)) {
                builder.agent(new Agent(agentName, agentVersion));
            }
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
            String buildUrl = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_URL);
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
            boolean runLicenseChecks = true;
            String runChecks = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_RUN_CHECKS);
            if (StringUtils.isNotBlank(runChecks)) {
                runLicenseChecks = Boolean.parseBoolean(runChecks);
            }
            LicenseControl licenseControl = new LicenseControl(runLicenseChecks);
            String notificationRecipients =
                    mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_VIOLATION_RECIPIENTS);
            if (StringUtils.isNotBlank(notificationRecipients)) {
                licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
            }
            String includePublishedArtifacts =
                    mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_INCLUDE_PUBLISHED_ARTIFACTS);
            if (StringUtils.isNotBlank(includePublishedArtifacts)) {
                licenseControl.setIncludePublishedArtifacts(Boolean.parseBoolean(includePublishedArtifacts));
            }
            String scopes = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_SCOPES);
            if (StringUtils.isNotBlank(scopes)) {
                licenseControl.setScopesList(scopes);
            }
            String autoDiscover = mergedProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_AUTO_DISCOVER);
            if (StringUtils.isNotBlank(autoDiscover)) {
                licenseControl.setAutoDiscover(Boolean.parseBoolean(autoDiscover));
            }
            builder.licenseControl(licenseControl);
            BuildRetention buildRetention = new BuildRetention();
            String buildRetentionDays = mergedProps.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_DAYS);
            if (StringUtils.isNotBlank(buildRetentionDays)) {
                buildRetention.setCount(Integer.parseInt(buildRetentionDays));
            }
            String buildRetentionMinimumDays = mergedProps.getProperty(
                    BuildInfoProperties.PROP_BUILD_RETENTION_MINIMUM_DATE);
            if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
                int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
                if (minimumDays > -1) {
                    Calendar calendar = Calendar.getInstance();
                    calendar.roll(Calendar.DAY_OF_YEAR, -minimumDays);
                    buildRetention.setMinimumBuildDate(calendar.getTime());
                }
            }
            builder.buildRetention(buildRetention);
            Properties props = BuildInfoExtractorUtils.getEnvProperties(mergedProps);
            Properties propsFromSys = BuildInfoExtractorUtils
                    .filterDynamicProperties(mergedProps, BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);
            props.putAll(propsFromSys);
            props = BuildInfoExtractorUtils.stripPrefixFromProperties(props, BUILD_INFO_PROP_PREFIX);
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
                    IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                            mergedProps.getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_INCLUDE_PATTERNS),
                            mergedProps.getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT_EXCLUDE_PATTERNS));

                    deployArtifacts(project, client, deployDetails, patterns);
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

    private void deployArtifacts(Project project, ArtifactoryBuildInfoClient client, Set<DeployDetails> deployDetails,
            IncludeExcludePatterns patterns) throws IOException {
        for (DeployDetails deployDetail : deployDetails) {
            String artifactPath = deployDetail.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                project.log("Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.", Project.MSG_INFO);
                continue;
            }
            client.deployArtifact(deployDetail);
        }
    }

    private void deployBuildInfo(ArtifactoryBuildInfoClient client, Build build) throws IOException {
        client.sendBuildInfo(build);
    }
}
