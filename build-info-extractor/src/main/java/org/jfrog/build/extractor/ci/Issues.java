package org.jfrog.build.extractor.ci;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jfrog.build.extractor.ci.IssueTracker.ToBuildInfoIssueTracker;

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

    /**
     * If one of the objects did not collect issues (empty tracker or affected issues) - keep the other.
     * If both collected and the tracker names match, append affected issues.
     * Otherwise, keep this original object.
     */
    public void append(Issues other) {
        if (other == null || other.getTracker() == null || other.affectedIssues == null) {
            return;
        }
        if (this.getTracker() == null || this.affectedIssues == null) {
            this.setTracker(other.getTracker());
            this.aggregateBuildIssues = other.aggregateBuildIssues;
            this.aggregationBuildStatus = other.aggregationBuildStatus;
            this.affectedIssues = other.affectedIssues;
            return;
        }
        if (other.getTracker().getName().equals(this.getTracker().getName())) {
            this.appendAffectedIssues(other.affectedIssues);
        }
    }

    private void appendAffectedIssues(Set<Issue> otherAffectedIssues) {
        if (otherAffectedIssues == null) {
            return;
        }
        if (this.affectedIssues == null) {
            this.affectedIssues = otherAffectedIssues;
            return;
        }
        this.affectedIssues.addAll(otherAffectedIssues);
    }

    public org.jfrog.build.api.Issues ToBuildIssues() {
        org.jfrog.build.api.Issues result = new org.jfrog.build.api.Issues();
        result.setAffectedIssues(affectedIssues == null ? null:affectedIssues.stream().map(Issue::ToBuildIssue).collect(Collectors.toSet()));
        result.setAggregateBuildIssues(aggregateBuildIssues);
        result.setTracker(tracker == null ? null :tracker.ToBuildIssueTracker());
        result.setAggregationBuildStatus(aggregationBuildStatus);
        return result;
    }


    public static Issues ToBuildInfoIssues(org.jfrog.build.api.Issues issues) {
        Issues result = new org.jfrog.build.extractor.ci.Issues();
        result.setAffectedIssues( issues.getAffectedIssues() == null ? null : issues.getAffectedIssues().stream().map(Issue::ToBuildInfoIssue).collect(Collectors.toSet()));
        result.setAggregateBuildIssues(issues.isAggregateBuildIssues());
        result.setTracker(issues.getTracker() == null ? null :ToBuildInfoIssueTracker(issues.getTracker()));
        result.setAggregationBuildStatus(issues.getAggregationBuildStatus());
        return result;
    }
}
