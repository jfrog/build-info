package org.jfrog.build.api;

/**
 * All the issue tracker related fields which exist inside the build info JSON
 *
 * @author Shay Yaakov
 */
public interface IssuesTrackerFields {
    String ISSUES_TRACKER_NAME = "tracker.name";
    String ISSUES_TRACKER_VERSION = "tracker.version";
    String AFFECTED_ISSUES = "affectedIssues";
    String AGGREGATE_BUILD_ISSUES = "aggregateBuildIssues";
    String AGGREGATION_BUILD_STATUS = "aggregationBuildStatus";
}
