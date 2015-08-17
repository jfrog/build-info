package org.jfrog.build.extractor.maven;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.Maven;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
                artifactoryPrincipal(clientConf.publisher.getName()).artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion()).
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
        String vcsUrl = clientConf.info.getVcsUrl();
        if (StringUtils.isNotBlank(vcsUrl)) {
            builder.vcsUrl(vcsUrl);
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
        attachStagingIfNeeded(clientConf, builder);
        builder.buildRetention(buildRetention);
        builder.artifactoryPrincipal(clientConf.publisher.getName());

        builder.artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion());

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

        for (Map.Entry<String, String> runParam : clientConf.info.getRunParameters().entrySet()) {
            MatrixParameter matrixParameter = new MatrixParameter(runParam.getKey(), runParam.getValue());
            builder.addRunParameters(matrixParameter);
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
        InputStream inputStream = BuildInfoRecorder.class.getClassLoader().
                getResourceAsStream("org/apache/maven/messages/build.properties");
        if (inputStream == null) {
            inputStream = Maven.class.getClassLoader().
                    getResourceAsStream("META-INF/maven/org.apache.maven/maven-core/pom.properties");
        }
        if (inputStream == null) {
            throw new RuntimeException("Could not extract Maven version: unable to find resources " +
                    "'org/apache/maven/messages/build.properties' or 'META-INF/maven/org.apache.maven/maven-core/pom.properties'");
        }
        try {
            mavenVersionProperties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Error while extracting Maven version properties from: org/apache/maven/messages/build.properties",
                    e);
        } finally {
            IOUtils.closeQuietly(inputStream);
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