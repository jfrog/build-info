package org.jfrog.build.extractor.maven;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoProperties.*;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoModelPropertyResolver.class)
public class BuildInfoModelPropertyResolver extends AbstractPropertyResolver<BuildInfoBuilder> {

    @Requirement
    private Logger logger;

    @Override
    public BuildInfoBuilder resolveProperties(Properties allProps) {
        Properties buildInfoProps =
                BuildInfoExtractorUtils.filterDynamicProperties(allProps, BuildInfoExtractorUtils.BUILD_INFO_PREDICATE);

        BuildInfoBuilder builder = resolveCoreProperties(allProps).
                url(buildInfoProps.getProperty(PROP_BUILD_URL)).
                agent(new Agent(buildInfoProps.getProperty(PROP_AGENT_NAME),
                        buildInfoProps.getProperty(PROP_AGENT_VERSION))).
                buildAgent(new BuildAgent("Maven", getMavenVersion())).
                principal(buildInfoProps.getProperty(PROP_PRINCIPAL)).
                vcsRevision(buildInfoProps.getProperty(PROP_VCS_REVISION)).
                parentName(buildInfoProps.getProperty(PROP_PARENT_BUILD_NAME)).
                parentNumber(buildInfoProps.getProperty(PROP_PARENT_BUILD_NUMBER)).
                properties(gatherBuildInfoProperties(allProps));

        resolveArtifactoryPrincipalProperty(allProps, builder);
        return builder;
    }

    private BuildInfoBuilder resolveCoreProperties(Properties buildInfoProperties) {
        String buildName = buildInfoProperties.getProperty(PROP_BUILD_NAME);
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Model properties: no build name was found.");
        }

        String buildNumber = buildInfoProperties.getProperty(PROP_BUILD_NUMBER);
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Model properties: no build number was found.");
        }

        String buildStarted = buildInfoProperties.getProperty(PROP_BUILD_STARTED);
        if (StringUtils.isBlank(buildStarted)) {
            throw new IllegalArgumentException(
                    "Unable to resolve Artifactory Build Info Model properties: no build started date was found.");
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
            IOUtils.closeQuietly(inputStream);
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
}