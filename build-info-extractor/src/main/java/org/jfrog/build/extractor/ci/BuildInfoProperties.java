package org.jfrog.build.extractor.ci;

/**
 * @author Tomer Cohen
 */
public interface BuildInfoProperties {

    /**
     * A prefix for all properties that should affect the build info model
     */
    String BUILD_INFO_PREFIX = "buildInfo.";

    /**
     * Prefix for properties that are dynamically added to build info
     */
    String BUILD_INFO_PROP_PREFIX = BUILD_INFO_PREFIX + "property.";
    String BUILD_INFO_ISSUES_TRACKER_PREFIX = BUILD_INFO_PREFIX + "issues.";

    /**
     * Prefix for build info properties that are coming from the CI server.
     */
    String BUILD_INFO_ENVIRONMENT_PREFIX = BUILD_INFO_PREFIX + "env.";
}