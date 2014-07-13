package org.jfrog.build.extractor.listener;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.Ivy;
import org.apache.ivy.ant.IvyAntSettings;
import org.apache.ivy.core.event.EventManager;
import org.apache.ivy.core.event.publish.EndArtifactPublishEvent;
import org.apache.ivy.core.event.resolve.EndResolveEvent;
import org.apache.ivy.plugins.trigger.Trigger;
import org.apache.tools.ant.*;
import org.apache.tools.ant.taskdefs.Ant;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.*;
import org.jfrog.build.context.BuildContext;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.trigger.ArtifactoryBuildInfoTrigger;
import org.jfrog.build.util.IvyBuildInfoLog;

import java.io.File;
import java.io.IOException;
import java.util.*;


/**
 * A listener which listens to the {@link Ant} builds, and is invoking different events during the build of {@code Ant}
 * itself! This is not to be confused with {@code Ivy} {@link Trigger} which is called during Ivy related events
 *
 * @author Tomer Cohen
 */
public class ArtifactoryBuildListener implements BuildListener {
    private static final ArtifactoryBuildInfoTrigger DEPENDENCY_TRIGGER = new ArtifactoryBuildInfoTrigger(EndResolveEvent.NAME);
    private static final ArtifactoryBuildInfoTrigger PUBLISH_TRIGGER = new ArtifactoryBuildInfoTrigger(EndArtifactPublishEvent.NAME);

    private boolean isDidDeploy;
    private BuildContext ctx;
    private IvyBuildInfoLog buildInfoLog;

    private void assertInitialized(BuildEvent event) {
        if (buildInfoLog != null) {
            buildInfoLog.setProject(event.getProject());
            return;
        }
        try {
            buildInfoLog = new IvyBuildInfoLog(event.getProject());
            ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(buildInfoLog);
            Properties props = getMergedEnvAndSystemProps();
            clientConf.fillFromProperties(props);
            ctx = new BuildContext(clientConf);
            DEPENDENCY_TRIGGER.setIvyBuildContext(ctx);
            PUBLISH_TRIGGER.setIvyBuildContext(ctx);
            buildInfoLog.info("[buildinfo:ant] Artifactory Build Info Listener Initialized");
        } catch (Exception e) {
            RuntimeException re = new RuntimeException(
                    "Fail to initialize the Ivy listeners for the Artifactory Ivy plugin, due to: " + e.getMessage(),
                    e);
            if (buildInfoLog != null && buildInfoLog.getProject() != null) {
                buildInfoLog.error(re.getMessage(), e);
            }
            throw re;
        }
    }

    private Properties getMergedEnvAndSystemProps() {
        Properties props = new Properties();
        props.putAll(System.getenv());
        return BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(props);
    }

    public IvyBuildInfoLog getBuildInfoLog(BuildEvent event) {
        assertInitialized(event);
        return buildInfoLog;
    }

    @Override
    public void buildStarted(BuildEvent event) {
        try {
            getBuildInfoLog(event).debug("[buildinfo:ant] Received Build Started Event");
            ctx.setBuildStartTime(System.currentTimeMillis());
            getBuildInfoLog(event).info("[buildinfo:ant] Build Started timestamp=" + ctx.getBuildStartTime());
        } catch (Exception e) {
            RuntimeException re = new RuntimeException("Fail to register start of build, due to: " + e.getMessage(), e);
            getBuildInfoLog(event).error(re.getMessage(), e);
            throw re;
        }
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
        try {
            if (event.getException() != null) {
                getBuildInfoLog(event).info(
                        "[buildinfo:ant] Received Build Finished Event with exception => No deployment");
                return;
            }
            getBuildInfoLog(event).debug("[buildinfo:ant] Received Build Finished Event");
            if (!isDidDeploy) {
                try {
                    doDeploy(event);
                } catch (Exception e) {
                    RuntimeException re = new RuntimeException(
                            "Fail to activate deployment using the Artifactory Ivy plugin, due to: " + e.getMessage(),
                            e);
                    getBuildInfoLog(event).error(re.getMessage(), e);
                    throw re;
                }
            }
        } finally {
            String propertyFilePath = System.getenv(BuildInfoConfigProperties.PROP_PROPS_FILE);
            if (StringUtils.isNotBlank(propertyFilePath)) {
                File file = new File(propertyFilePath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
    }

    private Task extractIvyTask(BuildEvent event) {
        Task task = event.getTask();
        // Interested only in Ivy tasks
        String taskType = task.getTaskType();
        if (taskType != null
                && (taskType.contains("org.apache.ivy") || taskType.contains("fr.jayasoft.ivy"))) {
            return task;
        }
        return null;
    }

    @Override
    public void taskStarted(BuildEvent event) {
        try {
            Task task = extractIvyTask(event);
            if (task != null) {
                String taskType = task.getTaskType();
                getBuildInfoLog(event).debug("[buildinfo:ant] Received Task of type '" + taskType + "' Started Event");
                // Need only retrieve, resolve, and publish tasks, since needs to give ivy settings a chance (BI-131)
                if (taskType.endsWith("retrieve") || taskType.endsWith("resolve")) {
                    getBuildInfoLog(event).debug("[buildinfo:ant] Adding Ivy Resolution Listeners if needed.");
                    List<EventManager> eventManagers = getEventManager(task);
                    for (EventManager eventManager : eventManagers) {
                        if (!eventManager.hasIvyListener(DEPENDENCY_TRIGGER)) {
                            eventManager.addIvyListener(DEPENDENCY_TRIGGER, DEPENDENCY_TRIGGER.getEventFilter());
                            getBuildInfoLog(event).info("[buildinfo:ant] Added resolution report Ivy Listener.");
                        }
                    }
                }
                if (taskType.endsWith("publish")) {
                    getBuildInfoLog(event).debug("[buildinfo:ant] Adding Ivy Publish Listeners if needed.");
                    List<EventManager> eventManagers = getEventManager(task);
                    for (EventManager eventManager : eventManagers) {
                        if (!eventManager.hasIvyListener(PUBLISH_TRIGGER)) {
                            eventManager.addIvyListener(PUBLISH_TRIGGER, PUBLISH_TRIGGER.getEventFilter());
                            getBuildInfoLog(event).info("[buildinfo:ant] Added publish end Ivy Listener to Ivy Engine.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            RuntimeException re = new RuntimeException(
                    "Fail to add the Ivy listeners for the Artifactory Ivy plugin, due to: " + e.getMessage(), e);
            getBuildInfoLog(event).error(re.getMessage(), e);
            throw re;
        }
    }

    /**
     * Retrieves all the event managers of all the ivy settings of the project.
     *
     * @param task the task we need to attach listeners to
     * @return the list of event manager for this task
     */
    private List<EventManager> getEventManager(Task task) {
        List<EventManager> results = new ArrayList<EventManager>();
        Project project = task.getProject();
        Enumeration<Object> elements = project.getReferences().elements();
        while (elements.hasMoreElements()) {
            Object element = elements.nextElement();
            if (element instanceof IvyAntSettings) {
                results.add(((IvyAntSettings) element).getConfiguredIvyInstance(task).getResolveEngine().getEventManager());
            }
        }
        if (results.isEmpty()) {
            throw new BuildException("Did not find ivy settings reference for project " + project.getName());
        }
        return results;
    }

    @Override
    public void taskFinished(BuildEvent event) {
        Task task = extractIvyTask(event);
        if (task != null) {
            getBuildInfoLog(event).debug("[buildinfo:ant] Received Task " + task.getTaskType() + " Finished Event");
        }
    }

    @Override
    public void targetStarted(BuildEvent event) {
    }

    @Override
    public void targetFinished(BuildEvent event) {
    }

    @Override
    public void messageLogged(BuildEvent event) {
    }

    private void doDeploy(BuildEvent event) {
        IvyBuildInfoLog log = getBuildInfoLog(event);
        log.info("[buildinfo:ant] Starting deployment");
        Project project = event.getProject();
        Set<DeployDetails> deployDetails = ctx.getDeployDetails();
        BuildInfoBuilder builder = new BuildInfoBuilder(project.getName()).modules(ctx.getModules())
                .number("0").durationMillis(System.currentTimeMillis() - ctx.getBuildStartTime())
                .startedDate(new Date(ctx.getBuildStartTime()))
                .buildAgent(new BuildAgent("Ivy", Ivy.getIvyVersion()))
                .agent(new Agent("Ivy", Ivy.getIvyVersion()));
        // This is here for backwards compatibility.
        builder.type(BuildType.IVY);
        ArtifactoryClientConfiguration clientConf = ctx.getClientConf();
        String agentName = clientConf.info.getAgentName();
        String agentVersion = clientConf.info.getAgentVersion();
        if (StringUtils.isNotBlank(agentName) && StringUtils.isNotBlank(agentVersion)) {
            builder.agent(new Agent(agentName, agentVersion));
        }
        String buildAgentName = clientConf.info.getBuildAgentName();
        String buildAgentVersion = clientConf.info.getBuildAgentVersion();
        if (StringUtils.isNotBlank(buildAgentName) && StringUtils.isNotBlank(buildAgentVersion)) {
            builder.buildAgent(new BuildAgent(buildAgentName, buildAgentVersion));
        }
        String buildName = clientConf.info.getBuildName();
        if (StringUtils.isNotBlank(buildName)) {
            builder.name(buildName);
        }
        String buildNumber = clientConf.info.getBuildNumber();
        if (StringUtils.isNotBlank(buildNumber)) {
            builder.number(buildNumber);
        }
        String buildUrl = clientConf.info.getBuildUrl();
        if (StringUtils.isNotBlank(buildUrl)) {
            builder.url(buildUrl);
        }
        String vcsRevision = clientConf.info.getVcsRevision();
        if (StringUtils.isNotBlank(vcsRevision)) {
            builder.vcsRevision(vcsRevision);
        }
        String vcsUrl = clientConf.info.getVcsUrl();
        if (StringUtils.isNotBlank(vcsUrl)) {
            builder.vcsUrl(vcsUrl);
        }
        String principal = clientConf.info.getPrincipal();
        if (StringUtils.isNotBlank(principal)) {
            builder.principal(principal);
        }
        String parentBuildName = clientConf.info.getParentBuildName();
        if (StringUtils.isNotBlank(parentBuildName)) {
            builder.parentName(parentBuildName);
        }
        String parentBuildNumber = clientConf.info.getParentBuildNumber();
        if (StringUtils.isNotBlank(parentBuildNumber)) {
            builder.parentNumber(parentBuildNumber);
        }
        LicenseControl licenseControl = new LicenseControl(clientConf.info.licenseControl.isRunChecks());
        String notificationRecipients = clientConf.info.licenseControl.getViolationRecipients();
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        licenseControl.setIncludePublishedArtifacts(clientConf.info.licenseControl.isIncludePublishedArtifacts());
        String scopes = clientConf.info.licenseControl.getScopes();
        if (StringUtils.isNotBlank(scopes)) {
            licenseControl.setScopesList(scopes);
        }
        licenseControl.setAutoDiscover(clientConf.info.licenseControl.isAutoDiscover());
        builder.licenseControl(licenseControl);

        final BlackDuckProperties blackDuckProperties;
        if (clientConf.info.blackDuckProperties.isRunChecks()) {
            blackDuckProperties = clientConf.info.blackDuckProperties.copyBlackDuckProperties();
        } else {
            blackDuckProperties = new BlackDuckProperties();
        }

        Governance governance = new Governance();
        governance.setBlackDuckProperties(blackDuckProperties);
        builder.governance(governance);

        BuildRetention buildRetention = new BuildRetention(clientConf.info.isDeleteBuildArtifacts());
        if (clientConf.info.getBuildRetentionCount() != null) {
            buildRetention.setCount(clientConf.info.getBuildRetentionCount());
        }
        String buildRetentionMinimumDays = clientConf.info.getBuildRetentionMinimumDate();
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        String[] notToDelete = clientConf.info.getBuildNumbersNotToDelete();
        for (String notToDel : notToDelete) {
            buildRetention.addBuildNotToBeDiscarded(notToDel);
        }
        builder.buildRetention(buildRetention);

        String issueTrackerName = clientConf.info.issues.getIssueTrackerName();
        if (StringUtils.isNotBlank(issueTrackerName)) {
            Issues issues = new Issues();
            issues.setAggregateBuildIssues(clientConf.info.issues.getAggregateBuildIssues());
            issues.setAggregationBuildStatus(clientConf.info.issues.getAggregationBuildStatus());
            issues.setTracker(new IssueTracker(issueTrackerName, clientConf.info.issues.getIssueTrackerVersion()));
            Set<Issue> affectedIssuesSet = clientConf.info.issues.getAffectedIssuesSet();
            if (!affectedIssuesSet.isEmpty()) {
                issues.setAffectedIssues(affectedIssuesSet);
            }
            builder.issues(issues);
        }

        if (clientConf.isIncludeEnvVars()) {
            Properties envProperties = new Properties();
            envProperties.putAll(clientConf.getAllProperties());
            envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties);
            for (Map.Entry<Object, Object> envProp : envProperties.entrySet()) {
                builder.addProperty(envProp.getKey(), envProp.getValue());
            }
        }
        Build build = builder.build();
        String contextUrl = clientConf.publisher.getContextUrl();
        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();
        try {
            ArtifactoryBuildInfoClient client =
                    new ArtifactoryBuildInfoClient(contextUrl, username, password, log);


            configureProxy(clientConf, client);
            if (clientConf.publisher.isPublishArtifacts()) {
                IncludeExcludePatterns patterns = new IncludeExcludePatterns(
                        clientConf.publisher.getIncludePatterns(), clientConf.publisher.getExcludePatterns());

                deployArtifacts(project, client, deployDetails, patterns);
            }
            if (clientConf.publisher.isPublishBuildInfo()) {
                client.sendBuildInfo(build);
            }
            isDidDeploy = true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deployArtifacts(Project project, ArtifactoryBuildInfoClient client, Set<DeployDetails> deployDetails,
                                 IncludeExcludePatterns patterns) throws IOException {
        for (DeployDetails deployDetail : deployDetails) {
            String artifactPath = deployDetail.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, patterns)) {
                project.log("[buildinfo:deploy] Skipping the deployment of '" + artifactPath +
                        "' due to the defined include-exclude patterns.", Project.MSG_INFO);
                continue;
            }
            client.deployArtifact(deployDetail);
        }
    }

    protected void configureProxy(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        ArtifactoryClientConfiguration.ProxyHandler proxy = clientConf.proxy;
        String proxyHost = proxy.getHost();
        if (StringUtils.isNotBlank(proxyHost) && proxy.getPort() != null) {
            buildInfoLog.debug("Found proxy host '" + proxyHost + "'");
            String proxyUserName = proxy.getUsername();
            if (StringUtils.isNotBlank(proxyUserName)) {
                buildInfoLog.debug("Found proxy user name '" + proxyUserName + "'");
                client.setProxyConfiguration(proxyHost, proxy.getPort(), proxyUserName, proxy.getPassword());
            } else {
                buildInfoLog.debug("No proxy user name and password found, using anonymous proxy");
                client.setProxyConfiguration(proxyHost, proxy.getPort());
            }
        }
    }
}
