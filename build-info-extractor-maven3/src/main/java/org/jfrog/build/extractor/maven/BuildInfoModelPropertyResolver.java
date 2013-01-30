package org.jfrog.build.extractor.maven;

import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.BlackDuckProperties;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Issue;
import org.jfrog.build.api.IssueTracker;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.LicenseControl;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.client.ArtifactoryClientConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoFields.*;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoModelPropertyResolver.class)
public class BuildInfoModelPropertyResolver {

    @Requirement
    private Logger logger;


    public BuildInfoMavenBuilder resolveProperties(ExecutionEvent event, ArtifactoryClientConfiguration clientConf) {
        BuildInfoMavenBuilder builder = resolveCoreProperties(event, clientConf).
                artifactoryPrincipal(clientConf.publisher.getName()).
                principal(clientConf.info.getPrincipal()).type(BuildType.MAVEN).parentName(
                clientConf.info.getParentBuildName()).
                parentNumber(clientConf.info.getParentBuildNumber());

        String buildUrl = clientConf.info.getBuildUrl();
        if (StringUtils.isNotBlank(buildUrl)) {
            builder.url(buildUrl);
        }
        String vcsRevision = clientConf.info.getVcsRevision();
        if (StringUtils.isNotBlank(vcsRevision)) {
            builder.vcsRevision(vcsRevision);
        }
        BuildAgent buildAgent = new BuildAgent("Maven", getMavenVersion());
        builder.buildAgent(buildAgent);

        String agentName = clientConf.info.getAgentName();
        if (StringUtils.isBlank(agentName)) {
            agentName = buildAgent.getName();
        }
        String agentVersion = clientConf.info.getAgentVersion();
        if (StringUtils.isBlank(agentVersion)) {
            agentVersion = buildAgent.getVersion();
        }
        builder.agent(new Agent(agentName, agentVersion));
        LicenseControl licenseControl = new LicenseControl(clientConf.info.licenseControl.isRunChecks());
        String notificationRecipients = clientConf.info.licenseControl.getViolationRecipients();
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        licenseControl.setIncludePublishedArtifacts(clientConf.info.licenseControl.isIncludePublishedArtifacts());
        licenseControl.setScopesList(clientConf.info.licenseControl.getScopes());
        licenseControl.setAutoDiscover(clientConf.info.licenseControl.isAutoDiscover());
        builder.licenseControl(licenseControl);

        BlackDuckProperties blackDuckProperties = new BlackDuckProperties();
        blackDuckProperties.setRunChecks(clientConf.info.blackDuckProperties.isRunChecks());
        blackDuckProperties.setAppName(clientConf.info.blackDuckProperties.getAppName());
        blackDuckProperties.setAppVersion(clientConf.info.blackDuckProperties.getAppVersion());
        blackDuckProperties.setReportRecipients(clientConf.info.blackDuckProperties.getReportRecipients());
        blackDuckProperties.setScopes(clientConf.info.blackDuckProperties.getScopes());
        blackDuckProperties.setIncludePublishedArtifacts(clientConf.info.blackDuckProperties.isIncludePublishedArtifacts());
        blackDuckProperties.setDisableComplianceAutoCheck(clientConf.info.blackDuckProperties.isDisableComplianceAutoCheck());
        builder.blackDuckProperties(blackDuckProperties);

        BuildRetention buildRetention = new BuildRetention(clientConf.info.isDeleteBuildArtifacts());
        if (clientConf.info.getBuildRetentionDays() != null) {
            buildRetention.setCount(clientConf.info.getBuildRetentionDays());
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
        attachStagingIfNeeded(clientConf, builder);
        builder.buildRetention(buildRetention);
        builder.artifactoryPrincipal(clientConf.publisher.getName());

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

        return builder;
    }

    private void attachStagingIfNeeded(ArtifactoryClientConfiguration clientConf, BuildInfoMavenBuilder builder) {
        if (clientConf.info.isReleaseEnabled()) {
            String stagingRepository = clientConf.publisher.getRepoKey();
            String comment = clientConf.info.getReleaseComment();
            if (comment == null) {
                comment = "";
            }
            String buildStartedIso = clientConf.info.getBuildStarted();
            Date buildStartDate;
            try {
                buildStartDate = new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStartedIso);
            } catch (ParseException e) {
                throw new IllegalArgumentException("Build start date format error: " + buildStartedIso, e);
            }
            builder.addStatus(new PromotionStatusBuilder(Promotion.STAGED).timestampDate(buildStartDate)
                    .comment(comment).repository(stagingRepository)
                    .ciUser(clientConf.info.getPrincipal()).user(clientConf.publisher.getUsername()).build());
        }
    }

    private BuildInfoMavenBuilder resolveCoreProperties(ExecutionEvent event,
            ArtifactoryClientConfiguration clientConf) {
        String buildName = clientConf.info.getBuildName();
        if (StringUtils.isBlank(buildName)) {
            buildName = event.getSession().getTopLevelProject().getName();
        }
        String buildNumber = clientConf.info.getBuildNumber();
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = Long.toString(System.currentTimeMillis());
        }
        Date buildStartedDate = event.getSession().getRequest().getStartTime();
        String buildStarted = clientConf.info.getBuildStarted();
        if (StringUtils.isBlank(buildStarted)) {
            buildStarted = new SimpleDateFormat(Build.STARTED_FORMAT).format(buildStartedDate);
        }

        String buildTimestamp = clientConf.info.getBuildTimestamp();
        if (StringUtils.isBlank(buildTimestamp)) {
            buildTimestamp = Long.toString(buildStartedDate.getTime());
        }
        logResolvedProperty(BUILD_NAME, buildName);
        logResolvedProperty(BUILD_NUMBER, buildNumber);
        logResolvedProperty(BUILD_STARTED, buildStarted);
        logResolvedProperty(BUILD_TIMESTAMP, buildTimestamp);
        return new BuildInfoMavenBuilder(buildName).number(buildNumber).started(buildStarted);
    }

    private String getMavenVersion() {
        Properties mavenVersionProperties = new Properties();
        InputStream inputStream = BuildInfoRecorder.class.getClassLoader()
                .getResourceAsStream("org/apache/maven/messages/build.properties");
        if (inputStream == null) {
            throw new RuntimeException("Could not extract Maven version: unable to find the resource " +
                    "'org/apache/maven/messages/build.properties'");
        }
        try {
            mavenVersionProperties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error while extracting Maven version properties from: org/apache/maven/messages/build.properties",
                    e);
        } finally {
            Closeables.closeQuietly(inputStream);
        }

        String version = mavenVersionProperties.getProperty("version");
        if (StringUtils.isBlank(version)) {
            throw new RuntimeException("Could not extract Maven version: no version property found in the resource " +
                    "'org/apache/maven/messages/build.properties'");
        }
        return version;
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Build Info Model Property Resolver: " + key + " = " + value);
    }
}