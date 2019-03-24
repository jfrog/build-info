package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * @author yahavi
 */
public class Issue implements Comparable<Issue> {

    private String created;
    private String description;
    private String issueType = "N/A";
    private String provider;
    private Severity severity = Severity.Normal;
    private String summary;
    private String component = "";

    @SuppressWarnings("WeakerAccess")
    public Issue() {
    }

    @SuppressWarnings("unused")
    public Issue(String created, String description, String issueType, String provider, Severity severity, String summary) {
        this.created = created;
        this.description = description;
        this.issueType = issueType;
        this.provider = provider;
        this.severity = severity;
        this.summary = summary;
    }

    @SuppressWarnings("WeakerAccess")
    public Severity getSeverity() {
        return this.severity;
    }

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getCreated() {
        return created;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getIssueType() {
        return issueType;
    }

    @SuppressWarnings("unused")
    public String getProvider() {
        return provider;
    }

    @SuppressWarnings("unused")
    public String getSummary() {
        return summary;
    }

    @JsonIgnore
    @SuppressWarnings("WeakerAccess")
    public boolean isTopSeverity() {
        return getSeverity() == Severity.High;
    }

    @JsonIgnore
    @SuppressWarnings("WeakerAccess")
    public boolean isHigherSeverityThan(Issue o) {
        return getSeverity().isHigherThan(o.getSeverity());
    }

    @Override
    public int compareTo(@Nonnull Issue otherIssue) {
        return Integer.compare(hashCode(), Objects.hashCode(otherIssue));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Issue otherIssue = (Issue) other;
        if (StringUtils.isEmpty(component)) {
            return StringUtils.equals(description, otherIssue.description) && StringUtils.equals(summary, otherIssue.summary);
        }
        return StringUtils.equals(component, otherIssue.component) && StringUtils.equals(summary, otherIssue.summary);
    }

    @Override
    public int hashCode() {
        return Objects.hash(summary, component, description);
    }
}
