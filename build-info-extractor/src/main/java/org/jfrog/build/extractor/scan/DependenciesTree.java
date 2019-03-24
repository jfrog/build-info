package org.jfrog.build.extractor.scan;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Dependencies tree for Xray scan. Used in 'Eclipse' and 'Idea' Xray plugins.
 *
 * @author yahavi
 */
public class DependenciesTree extends DefaultMutableTreeNode {

    private Set<Issue> issues = new HashSet<>();
    private Set<License> licenses = new HashSet<>();
    private GeneralInfo generalInfo;
    private Issue topIssue = new Issue();

    public DependenciesTree() {
        super();
    }

    public DependenciesTree(Object userObject) {
        super(userObject);
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
    }

    @SuppressWarnings("unused")
    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    @SuppressWarnings("unused")
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    public Set<Issue> getIssues() {
        return issues;
    }

    public Set<License> getLicenses() {
        return licenses;
    }

    /**
     * @return top severity issue of the current node and its ancestors
     */
    @SuppressWarnings("WeakerAccess")
    public Issue getTopIssue() {
        return topIssue;
    }

    /**
     * @return total number of issues of the current node and its ancestors
     */
    @SuppressWarnings("WeakerAccess")
    public int getIssueCount() {
        return issues.size();
    }

    /**
     * @return Node's children
     */
    @SuppressWarnings({"WeakerAccess", "unchecked"})
    public Vector<DependenciesTree> getChildren() {
        return children != null ? children : new Vector<>();
    }

    /**
     * 1. Populate current node's issues components
     * 2. Populate current node and subtree's issues
     * 3. Populate current node and subtree's top issue
     * 4. Sort the tree
     *
     * @return all issues of the current node and its ancestors
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    public Set<Issue> processTreeIssues() {
        setIssuesComponent();
        getChildren().forEach(child -> issues.addAll(child.processTreeIssues()));
        setTopIssue();
        sortChildren();
        return issues;
    }

    private void setIssuesComponent() {
        issues.forEach(issue -> issue.setComponent(getUserObject().toString()));
    }

    private void sortChildren() {
        getChildren().sort(Comparator
                .comparing(DependenciesTree::getTopIssue, Comparator.comparing(Issue::getSeverity))
                .thenComparing(DependenciesTree::getIssueCount)
                .thenComparing(DependenciesTree::getChildCount)
                .reversed()
                .thenComparing(DependenciesTree::toString));
    }

    private void setTopIssue() {
        issues.forEach(issue -> {
            if (topIssue.isTopSeverity()) {
                return;
            }
            if (issue.isHigherSeverityThan(topIssue)) {
                topIssue = issue;
            }
        });
    }
}
