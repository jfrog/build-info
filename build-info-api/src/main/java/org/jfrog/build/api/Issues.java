package org.jfrog.build.api;

import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.List;

/**
 * @author Noam Y. Tenne
 */
public class Issues implements Serializable {

    private IssueTracker tracker;
    private List<Issue> affectedIssues;

    public IssueTracker getTracker() {
        return tracker;
    }

    public void setTracker(IssueTracker tracker) {
        this.tracker = tracker;
    }

    public List<Issue> getAffectedIssues() {
        return affectedIssues;
    }

    public void addIssue(Issue issue) {
        if (affectedIssues == null) {
            affectedIssues = Lists.newArrayList();
        }
        affectedIssues.add(issue);
    }

    public void setAffectedIssues(List<Issue> affectedIssues) {
        this.affectedIssues = affectedIssues;
    }
}
