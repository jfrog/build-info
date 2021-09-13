package org.jfrog.build.extractor.scan;

import javax.swing.tree.DefaultMutableTreeNode;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.*;

/**
 * Dependency tree for Xray scan. Used in 'Eclipse' and 'Idea' Xray plugins.
 *
 * @author yahavi
 */
@JsonFilter("xray-graph-filter")
public class DependencyTree extends DefaultMutableTreeNode {

    private Set<License> licenses = new HashSet<>();
    private Set<Issue> issues = new HashSet<>();
    private Set<Scope> scopes = new HashSet<>();
    private Issue topIssue = new Issue();
    private GeneralInfo generalInfo = new GeneralInfo();

    public DependencyTree() {
        super();
    }

    public DependencyTree(Object userObject) {
        super(userObject);
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    @JsonProperty("component_id")
    public String getComponentId() {
        return generalInfo.getPrefix().toLowerCase() + "://" + this;
    }

    public void setScopes(Set<Scope> scopes) {
        this.scopes = scopes;
    }

    @SuppressWarnings("unused")
    public void setGeneralInfo(GeneralInfo generalInfo) {
        this.generalInfo = generalInfo;
    }

    public Set<License> getLicenses() {
        return licenses;
    }

    public Set<Issue> getIssues() {
        return issues;
    }

    public Set<Scope> getScopes() {
        return scopes;
    }

    @SuppressWarnings("unused")
    public GeneralInfo getGeneralInfo() {
        return generalInfo;
    }

    /**
     * @return top severity issue of the current node and its ancestors
     */
    @SuppressWarnings("WeakerAccess")
    public Issue getTopIssue() {
        return topIssue;
    }

    /**
     * @return if one or more of the licenses is violating define policy
     */
    public boolean isLicenseViolating() {
        if (licenses.stream().anyMatch(license -> license.getIsViolate())) {
            return true;
        }
        if (getChildren().size() > 0) {
            for(DependencyTree node:getNodes()){
                if(node.isLicenseViolating())
                    return true;
            }
        }
        return  false;
    }

    public void setPrefix(String prefix) {
        generalInfo.prefix(prefix);
        if (getChildren().size() > 0) {
            getNodes().stream().forEach(node -> node.setPrefix(prefix));
        }
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
    public Vector<DependencyTree> getChildren() {
        return children != null ? children : new Vector<>();
    }

    @JsonProperty(value = "nodes")
    public List<DependencyTree> getNodes() {
        return children;
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
        Object userObject = getUserObject();
        if (userObject != null) {
            issues.forEach(issue -> issue.setComponent(userObject.toString()));
        }
    }

    private void sortChildren() {
        getChildren().sort(Comparator
                .comparing(DependencyTree::getTopIssue, Comparator.comparing(Issue::getSeverity))
                .thenComparing(DependencyTree::getIssueCount)
                .thenComparing(DependencyTree::getChildCount)
                .reversed()
                .thenComparing(DependencyTree::toString));
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

    /**
     * Recursively, collect all scopes and licenses.
     *
     * @param allScopes   - Out - All dependency tree scopes
     * @param allLicenses - Out - All dependency tree licenses
     */
    @SuppressWarnings("unused")
    public void collectAllScopesAndLicenses(Set<Scope> allScopes, Set<License> allLicenses) {
        Enumeration<?> enumeration = breadthFirstEnumeration();
        while (enumeration.hasMoreElements()) {
            DependencyTree child = (DependencyTree) enumeration.nextElement();
            allScopes.addAll(child.getScopes());
            allLicenses.addAll(child.getLicenses());
        }
    }
}
