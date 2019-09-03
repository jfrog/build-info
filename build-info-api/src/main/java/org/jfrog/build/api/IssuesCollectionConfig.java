package org.jfrog.build.api;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Serializable;

/**
 * This class is used by the IssuesCollector to parse the configuration file for the collectIssues method.
 */
public class IssuesCollectionConfig implements Serializable {
    public static final String ISSUES_COLLECTION_ERROR_PREFIX = "Issues Collection: ";
    private static final String MISSING_CONFIGURATION_ERROR = "Configuration file must contain: ";

    /**
     * The schema version is intended for internal use.
     * */
    private int version;
    private Issues issues;

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Issues getIssues() {
        return issues;
    }

    public void setIssues(Issues issues) {
        this.issues = issues;
    }

    public void validateConfig() throws IOException {
        if (issues == null) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + MISSING_CONFIGURATION_ERROR + "issues");
        }
        if (issues.trackerName == null) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + MISSING_CONFIGURATION_ERROR + "trackerName");
        }
        if (issues.regexp == null) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + MISSING_CONFIGURATION_ERROR + "regexp");
        }
        if (issues.keyGroupIndex == 0) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + MISSING_CONFIGURATION_ERROR + "keyGroupIndex");
        }
        if (issues.summaryGroupIndex == 0) {
            throw new IOException(ISSUES_COLLECTION_ERROR_PREFIX + MISSING_CONFIGURATION_ERROR + "summaryGroupIndex");
        }
    }

    public static class Issues implements Serializable {
        /**
         * The name (type) of the issue tracking system. For example, JIRA. This property can take any value.
         * */
        private String trackerName;
        /**
         * A regular expression used for matching the git commit messages. The expression should include two capturing groups - for the issue key (ID) and the issue summary.
         * */
        private String regexp;
        /**
         * The capturing group index in the regular expression used for retrieving the issue key.
         * */
        private int keyGroupIndex;
        /**
         * The capturing group index in the regular expression for retrieving the issue summary.
         * */
        private int summaryGroupIndex;
        /**
         * The issue tracking URL. This value is used for constructing a direct link to the issues in the Artifactory build UI.
         * */
        private String trackerUrl;
        /**
         * Set to true, if you wish all builds to include issues from previous builds.
         * */
        private boolean aggregate;
        /**
         * If aggregate is set to true, this property indicates how far in time should the issues be aggregated.
         * */
        private String aggregationStatus;

        public String getTrackerName() {
            return trackerName;
        }

        public void setTrackerName(String trackerName) {
            this.trackerName = trackerName;
        }

        public String getRegexp() {
            return regexp;
        }

        public void setRegexp(String regexp) {
            this.regexp = regexp;
        }

        public int getKeyGroupIndex() {
            return keyGroupIndex;
        }

        public void setKeyGroupIndex(int keyGroupIndex) {
            this.keyGroupIndex = keyGroupIndex;
        }

        public int getSummaryGroupIndex() {
            return summaryGroupIndex;
        }

        public void setSummaryGroupIndex(int summaryGroupIndex) {
            this.summaryGroupIndex = summaryGroupIndex;
        }

        public String getTrackerUrl() {
            return trackerUrl;
        }

        public void setTrackerUrl(String trackerUrl) {
            this.trackerUrl = StringUtils.stripEnd(trackerUrl, "/");
        }

        public boolean isAggregate() {
            return aggregate;
        }

        public void setAggregate(boolean aggregate) {
            this.aggregate = aggregate;
        }

        public String getAggregationStatus() {
            return aggregationStatus;
        }

        public void setAggregationStatus(String aggregationStatus) {
            this.aggregationStatus = aggregationStatus;
        }
    }
}




