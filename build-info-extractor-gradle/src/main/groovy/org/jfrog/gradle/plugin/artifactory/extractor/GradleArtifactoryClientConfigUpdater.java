package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.StartParameter;
import org.gradle.api.Project;
import org.jfrog.build.extractor.ci.BuildInfoFields;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.util.PathSanitizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoProperties.BUILD_INFO_PROP_PREFIX;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE;
import static org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.addDefaultPublisherAttributes;

/**
 * Populator util for filling up an ArtifactoryClientConfiguration based on a gradle project + environment properties
 *
 * @author Tomer Cohen
 */
public class GradleArtifactoryClientConfigUpdater {

    /**
     * Returns a configuration handler object out of a Gradle project. This method will aggregate the properties in our
     * defined hierarchy. First search for the property as a system property, if found return it.
     * Second search for the property in the Gradle {@link org.gradle.StartParameter#getProjectProperties} container
     * and if found there, then return it. Third search for the property in {@link
     * org.gradle.api.Project#property(String)}
     * if not found, search upwards in the project hierarchy until
     * reach the root project. if not found at all in this hierarchy return null
     *
     * @param project the gradle project with properties for build info client configuration (Usually in start parameter
     *                from CI Server)
     */
    public static void update(ArtifactoryClientConfiguration config, Project project) {
        Properties props = new Properties();
        // First aggregate properties from parent to child
        fillProperties(project, props);
        // Then start parameters
        StartParameter startParameter = project.getGradle().getStartParameter();
        Map<String, String> startProps = startParameter.getProjectProperties();
        props.putAll(BuildInfoExtractorUtils.filterStringEntries(startProps));

        // Then System properties
        Properties mergedProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(props, config.info.getLog());

        // Override file loading with explicit system property check to avoid Gradle daemon caching issues.
        String buildInfoPropFile = System.getProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        String propSource = "system property (" + BuildInfoConfigProperties.PROP_PROPS_FILE + ")";

        if (StringUtils.isBlank(buildInfoPropFile)) {
            buildInfoPropFile = System.getProperty(BuildInfoConfigProperties.ENV_BUILDINFO_PROPFILE);
            propSource = "system property (" + BuildInfoConfigProperties.ENV_BUILDINFO_PROPFILE + ")";
        }

        // If we found a system property-based file path, reload it to ensure it takes priority over env var
        if (StringUtils.isNotBlank(buildInfoPropFile)) {
            Properties fileProps = loadBuildInfoProperties(buildInfoPropFile, config.info.getLog());
            if (fileProps != null) {
                mergedProps.putAll(fileProps);
                config.info.getLog().debug("Overriding with properties from " + propSource + ": " + buildInfoPropFile);
            }
        }
        
        // Then special buildInfo properties
        Properties buildInfoProperties =
                BuildInfoExtractorUtils.filterDynamicProperties(mergedProps, BUILD_INFO_PROP_PREDICATE);
        buildInfoProperties =
                BuildInfoExtractorUtils.stripPrefixFromProperties(buildInfoProperties, BUILD_INFO_PROP_PREFIX);
        mergedProps.putAll(buildInfoProperties);

        // Add the collected properties to the Artifactory client configuration.
        // In case the build name and build number have already been added to the configuration
        // from inside the gradle script, we do not want to override them by the values sent from
        // the CI server plugin.
        String prefix = BuildInfoProperties.BUILD_INFO_PREFIX;
        Set<String> excludeIfExist = CommonUtils.newHashSet(prefix + BuildInfoFields.BUILD_NUMBER, prefix + BuildInfoFields.BUILD_NAME, prefix + BuildInfoFields.BUILD_STARTED);
        config.fillFromProperties(mergedProps, excludeIfExist);

        // After props are set, apply missing project props (not set by CI-plugin generated props)
        addDefaultPublisherAttributes(config, project.getRootProject().getName(), "Gradle", project.getGradle().getGradleVersion());
    }

    /**
     * Helper method to load build info properties from a file with proper error handling
     */
    private static Properties loadBuildInfoProperties(String filePath, org.jfrog.build.api.util.Log log) {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        
        // Validate and sanitize path to prevent path traversal attacks
        File propertiesFile = PathSanitizer.validateAndNormalize(filePath);
        if (propertiesFile == null || !propertiesFile.exists()) {
            log.warn("Properties file not found at: " + filePath);
            return null;
        }
        
        try (InputStream inputStream = new FileInputStream(propertiesFile)) {
            Properties fileProps = new Properties();
            fileProps.load(inputStream);
            return fileProps;
        } catch (Exception e) {
            log.error("Failed to load properties from: " + filePath + " - " + e.getMessage());
            return null;
        }
    }

    private static void fillProperties(Project project, Properties props) {
        Project parent = project.getParent();
        if (parent != null) {
            // Parent first than me
            fillProperties(parent, props);
        }
        Map<String, ?> projectProperties = project.getExtensions().getExtraProperties().getProperties();
        props.putAll(BuildInfoExtractorUtils.filterStringEntries(projectProperties));
    }
}
