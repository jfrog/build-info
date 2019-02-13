package org.jfrog.build.api;

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
