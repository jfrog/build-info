package org.jfrog.build.extractor.ci;

/**
 * @author Tomer Cohen
 */
public interface BuildInfoConfigProperties {

    /**
     * Prefix for all config/runtime properties
     */
    String BUILD_INFO_CONFIG_PREFIX = "buildInfoConfig.";
    String PROPERTIES_FILE = "propertiesFile";
    String PROPERTIES_FILE_KEY = "propertiesFileKey";
    String PROPERTIES_FILE_KEY_IV = "propertiesFileKeyIv";

    String PROP_PROPS_FILE = BUILD_INFO_CONFIG_PREFIX + PROPERTIES_FILE;
    String PROP_PROPS_FILE_KEY = BUILD_INFO_CONFIG_PREFIX + PROPERTIES_FILE_KEY;
    String PROP_PROPS_FILE_KEY_IV = BUILD_INFO_CONFIG_PREFIX + PROPERTIES_FILE_KEY_IV;

    String EXPORT_FILE = "exportFile";
    String PROP_EXPORT_FILE_PATH = BUILD_INFO_CONFIG_PREFIX + EXPORT_FILE;

    String ACTIVATE_RECORDER = "org.jfrog.build.extractor.maven.recorder.activate";

    /**
     * Property for whether to include all environment variables in the generic set of build info properties
     */
    String INCLUDE_ENV_VARS = "includeEnvVars";

    String ENV_VARS_INCLUDE_PATTERNS = "envVarsIncludePatterns";
    String PROP_ENV_VARS_INCLUDE_PATTERNS = BUILD_INFO_CONFIG_PREFIX + ENV_VARS_INCLUDE_PATTERNS;

    String ENV_VARS_EXCLUDE_PATTERNS = "envVarsExcludePatterns";
    String PROP_ENV_VARS_EXCLUDE_PATTERNS = BUILD_INFO_CONFIG_PREFIX + ENV_VARS_EXCLUDE_PATTERNS;

    /**
     * Secondary environment variable to hold the properties file name
     */
    String ENV_BUILDINFO_PROPFILE = "BUILDINFO_PROPFILE";

    /**
     * Maven property which indicates whether to resolve dependencies from Artifactory.
     */
    String ARTIFACTORY_RESOLUTION_ENABLED = "artifactoryResolutionEnabled";

    String PROP_ARTIFACTORY_RESOLUTION_ENABLED = BUILD_INFO_CONFIG_PREFIX + ARTIFACTORY_RESOLUTION_ENABLED;
}