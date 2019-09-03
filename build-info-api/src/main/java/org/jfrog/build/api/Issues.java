package org.jfrog.build.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class Issues implements Serializable {

    private IssueTracker tracker;
    private boolean aggregateBuildIssues;
    private String aggregationBuildStatus;
    private Set<Issue> affectedIssues;

    public Issues() {
    }

    public Issues(IssueTracker tracker, boolean aggregateBuildIssues, String aggregationBuildStatus, Set<Issue> affectedIssues) {
        this.tracker = tracker;
        this.aggregateBuildIssues = aggregateBuildIssues;
        this.aggregationBuildStatus = aggregationBuildStatus;
        this.affectedIssues = affectedIssues;
    }

    public IssueTracker getTracker() {
        return tracker;
    }

    public void setTracker(IssueTracker tracker) {
        this.tracker = tracker;
    }

    public Set<Issue> getAffectedIssues() {
        return affectedIssues;
    }

    public void addIssue(Issue issue) {
        if (affectedIssues == null) {
            affectedIssues = new HashSet<>();
        }
        affectedIssues.add(issue);
    }

    @JsonIgnore
    public boolean isEmpty() {
        if (tracker != null && StringUtils.isNotEmpty(aggregationBuildStatus)) {
            return false;
        }
        if (affectedIssues == null) {
            return true;
        }
        return affectedIssues.isEmpty();
    }

    public void setAffectedIssues(Set<Issue> affectedIssues) {
        this.affectedIssues = affectedIssues;
    }

    public boolean isAggregateBuildIssues() {
        return aggregateBuildIssues;
    }

    public void setAggregateBuildIssues(boolean aggregateBuildIssues) {
        this.aggregateBuildIssues = aggregateBuildIssues;
    }

    public String getAggregationBuildStatus() {
        return aggregationBuildStatus;
    }

    public void setAggregationBuildStatus(String aggregationBuildStatus) {
        this.aggregationBuildStatus = aggregationBuildStatus;
    }
}
