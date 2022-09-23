package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency tree for Xray scan. Used in 'Eclipse' and 'Idea' Xray plugins.
 *
 * @author yahavi
 */
@JsonFilter("xray-graph-filter")
public class DependencyTree extends DefaultMutableTreeNode {

    private Set<License> violatedLicenses = new HashSet<>();
    private Set<License> licenses = new HashSet<>();
    private Set<Issue> issues = new HashSet<>();
    private Set<Scope> scopes = new HashSet<>();
    private Issue topIssue = new Issue();
    private GeneralInfo generalInfo;
    private String packagePrefix = "";

    /**
     * metadata should be true if one of the following statement is true:
     * Node represents the root of the dependency tree.
     * Node represents a project.
     * Node represents a module in project.
     */
    private boolean metadata;

    public DependencyTree() {
        super();
    }

    public DependencyTree(Object userObject) {
        super(userObject);
    }

    @SuppressWarnings("unused")
    public void setViolatedLicenses(Set<License> violatedLicenses) {
        this.violatedLicenses = violatedLicenses;
    }

    public Set<License> getViolatedLicenses() {
        return violatedLicenses;
    }

    public void setLicenses(Set<License> licenses) {
        this.licenses = licenses;
    }

    public void setIssues(Set<Issue> issues) {
        this.issues = issues;
    }

    @JsonProperty("component_id")
    @SuppressWarnings("unused")
    public String getComponentId() {
        return packagePrefix + this;
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

    @SuppressWarnings("unused")
    public boolean isMetadata() {
        return metadata;
    }

    @SuppressWarnings("unused")
    public void setMetadata(boolean metadata) {
        this.metadata = metadata;
    }

    public void setPrefix(String prefix) {
        packagePrefix = prefix.toLowerCase() + "://";
        getChildren().forEach(node -> node.setPrefix(prefix));
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
        return children != null ? (Vector)children : new Vector<>();
    }

    @JsonProperty(value = "nodes")
    @SuppressWarnings({"unchecked", "unused"})
    public List<DependencyTree> getNodes() {
        return (Vector)children;
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
     * 1. Populate current node's licenses components
     * 2. Populate current node and subtree's violated licenses
     *
     * @return all violated licenses of the current node and its ancestors
     */
    public Set<License> processTreeViolatedLicenses() {
        setViolatedLicensesComponent();
        violatedLicenses.addAll(licenses.stream().filter(License::isViolate).collect(Collectors.toSet()));
        getChildren().forEach(child -> violatedLicenses.addAll(child.processTreeViolatedLicenses()));
        return violatedLicenses;
    }

    private void setViolatedLicensesComponent() {
        Object userObject = getUserObject();
        if (userObject != null) {
            licenses.forEach(license -> license.setComponent(userObject.toString()));
        }
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

    /**
     * Recursively find a node contains the input component ID.
     *
     * @param componentId - The component ID to search
     * @return a node contains the input component ID or null.
     */
    public DependencyTree find(String componentId) {
        if (StringUtils.equals(toString(), componentId)) {
            return this;
        }
        return getChildren().stream()
                .map(child -> child.find(componentId))
                .filter(Objects::nonNull)
                .findAny()
                .orElse(null);
    }

    /**
     * Return true if the node contains a loop.
     *
     * @param logger - The logger
     * @return true if the node contains a loop
     */
    public boolean hasLoop(Log logger) {
        for (DependencyTree parent = (DependencyTree) getParent(); parent != null; parent = (DependencyTree) parent.getParent()) {
            if (Objects.equals(getUserObject(), parent.getUserObject())) {
                logger.debug("Loop detected in " + getUserObject());
                return true;
            }
        }
        return false;
    }
}
