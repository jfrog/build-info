package org.jfrog.build.extractor.maven;

import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.BuildInfoProperties;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.LicenseControl;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoProperties.*;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoModelPropertyResolver.class)
public class BuildInfoModelPropertyResolver {

    @Requirement
    private Logger logger;

    public BuildInfoBuilder resolveProperties(ExecutionEvent event, Properties allProps) {
        Properties buildInfoProps =
                BuildInfoExtractorUtils.filterDynamicProperties(allProps, BuildInfoExtractorUtils.BUILD_INFO_PREDICATE);

        Properties clientProps =
                BuildInfoExtractorUtils.filterDynamicProperties(allProps, BuildInfoExtractorUtils.CLIENT_PREDICATE);

        BuildInfoBuilder builder = resolveCoreProperties(event, allProps).
                artifactoryPrincipal(clientProps.getProperty(ClientProperties.PROP_PUBLISH_USERNAME)).
                url(buildInfoProps.getProperty(PROP_BUILD_URL)).
                principal(buildInfoProps.getProperty(PROP_PRINCIPAL)).
                type(BuildType.MAVEN).
                parentName(buildInfoProps.getProperty(PROP_PARENT_BUILD_NAME)).
                parentNumber(buildInfoProps.getProperty(PROP_PARENT_BUILD_NUMBER)).
                properties(gatherBuildInfoProperties(allProps));

        String vcsRevision = buildInfoProps.getProperty(PROP_VCS_REVISION);
        if (StringUtils.isNotBlank(vcsRevision)) {
            addMatrixParamIfNeeded(allProps, "vcs.revision", vcsRevision);
            builder.vcsRevision(vcsRevision);
        }

        BuildAgent buildAgent = new BuildAgent("Maven", getMavenVersion());
        builder.buildAgent(buildAgent);

        String agentName = buildInfoProps.getProperty(PROP_AGENT_NAME);
        if (StringUtils.isBlank(agentName)) {
            agentName = buildAgent.getName();
        }
        String agentVersion = buildInfoProps.getProperty(PROP_AGENT_VERSION);
        if (StringUtils.isBlank(agentVersion)) {
            agentVersion = buildAgent.getVersion();
        }
        builder.agent(new Agent(agentName, agentVersion));
        boolean runLicenseChecks = true;
        String runChecks = buildInfoProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_RUN_CHECKS);
        if (StringUtils.isNotBlank(runChecks)) {
            runLicenseChecks = Boolean.parseBoolean(runChecks);
        }
        LicenseControl licenseControl = new LicenseControl(runLicenseChecks);
        String notificationRecipients =
                buildInfoProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_VIOLATION_RECIPIENTS);
        if (StringUtils.isNotBlank(notificationRecipients)) {
            licenseControl.setLicenseViolationsRecipientsList(notificationRecipients);
        }
        String includePublishedArtifacts =
                buildInfoProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_INCLUDE_PUBLISHED_ARTIFACTS);
        if (StringUtils.isNotBlank(includePublishedArtifacts)) {
            licenseControl.setIncludePublishedArtifacts(Boolean.parseBoolean(includePublishedArtifacts));
        }
        String scopes = buildInfoProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_SCOPES);
        if (StringUtils.isNotBlank(scopes)) {
            licenseControl.setScopesList(scopes);
        }
        String autoDiscover = buildInfoProps.getProperty(BuildInfoProperties.PROP_LICENSE_CONTROL_AUTO_DISCOVER);
        if (StringUtils.isNotBlank(autoDiscover)) {
            licenseControl.setAutoDiscover(Boolean.parseBoolean(autoDiscover));
        }
        builder.licenseControl(licenseControl);
        BuildRetention buildRetention = new BuildRetention();
        String buildRetentionDays = buildInfoProps.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_DAYS);
        if (StringUtils.isNotBlank(buildRetentionDays)) {
            buildRetention.setCount(Integer.parseInt(buildRetentionDays));
        }
        String buildRetentionMinimumDays =
                buildInfoProps.getProperty(BuildInfoProperties.PROP_BUILD_RETENTION_MINIMUM_DATE);
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.roll(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        builder.buildRetention(buildRetention);
        resolveArtifactoryPrincipalProperty(allProps, builder);
        return builder;
    }

    private BuildInfoBuilder resolveCoreProperties(ExecutionEvent event, Properties allProps) {
        String buildName = allProps.getProperty(PROP_BUILD_NAME);
        if (StringUtils.isBlank(buildName)) {
            buildName = event.getSession().getTopLevelProject().getName();
        }
        addMatrixParamIfNeeded(allProps, "build.name", buildName);

        String buildNumber = allProps.getProperty(PROP_BUILD_NUMBER);
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = Long.toString(System.currentTimeMillis());
        }
        addMatrixParamIfNeeded(allProps, "build.number", buildNumber);

        String buildStarted = allProps.getProperty(PROP_BUILD_STARTED);
        if (StringUtils.isBlank(buildStarted)) {
            buildStarted =
                    new SimpleDateFormat(Build.STARTED_FORMAT).format(event.getSession().getRequest().getStartTime());
        }

        logResolvedProperty(PROP_BUILD_NAME, buildName);
        logResolvedProperty(PROP_BUILD_NUMBER, buildNumber);
        logResolvedProperty(PROP_BUILD_STARTED, buildStarted);
        return new BuildInfoBuilder(buildName).number(buildNumber).started(buildStarted);
    }

    private void resolveArtifactoryPrincipalProperty(Properties allProps, BuildInfoBuilder builder) {
        Properties clientProps =
                BuildInfoExtractorUtils.filterDynamicProperties(allProps, BuildInfoExtractorUtils.CLIENT_PREDICATE);
        builder.artifactoryPrincipal(clientProps.getProperty(ClientProperties.PROP_PUBLISH_USERNAME));
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

    private Properties gatherBuildInfoProperties(Properties allProps) {
        Properties props = new Properties();
        props.setProperty("os.arch", System.getProperty("os.arch"));
        props.setProperty("os.name", System.getProperty("os.name"));
        props.setProperty("os.version", System.getProperty("os.version"));
        props.setProperty("java.version", System.getProperty("java.version"));
        props.setProperty("java.vm.info", System.getProperty("java.vm.info"));
        props.setProperty("java.vm.name", System.getProperty("java.vm.name"));
        props.setProperty("java.vm.specification.name", System.getProperty("java.vm.specification.name"));
        props.setProperty("java.vm.vendor", System.getProperty("java.vm.vendor"));

        Properties propertiesToAttach = BuildInfoExtractorUtils
                .filterDynamicProperties(allProps, BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);
        for (Map.Entry<Object, Object> propertyToAttach : propertiesToAttach.entrySet()) {
            String key = StringUtils.removeStart(((String) propertyToAttach.getKey()), BUILD_INFO_PROP_PREFIX);
            props.setProperty(key, (String) propertyToAttach.getValue());
        }

        return props;
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Build Info Model Property Resolver: " + key + " = " + value);
    }

    private void addMatrixParamIfNeeded(Properties allProps, String paramPrefix, String paramValue) {
        String matrixParamKey = ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + paramPrefix;
        if (!allProps.containsKey(matrixParamKey)) {
            allProps.put(matrixParamKey, paramValue);
        }
    }
}