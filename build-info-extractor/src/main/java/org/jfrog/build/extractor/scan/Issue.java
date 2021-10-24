package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;

/**
 * @author yahavi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Issue implements Comparable<Issue> {

    private Severity severity = Severity.Normal;
    private List<String> fixedVersions;
    private String component = "";
    private String description;
    private String summary;

    public Issue() {
    }

    @SuppressWarnings("unused")
    public Issue(String description, Severity severity, String summary, List<String> fixedVersions) {
        this.description = description;
        this.severity = severity;
        this.summary = summary;
        this.fixedVersions = fixedVersions;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public String getComponent() {
        return this.component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    @SuppressWarnings("unused")
    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public String getSummary() {
        return summary;
    }

    @SuppressWarnings("unused")
    public List<String> getFixedVersions() {
        return fixedVersions;
    }

    @SuppressWarnings("unused")
    public void setFixedVersions(List<String> fixedVersions) {
        this.fixedVersions = fixedVersions;
    }

    @JsonIgnore
    @SuppressWarnings("WeakerAccess")
    public boolean isTopSeverity() {
        return getSeverity() == Severity.Critical;
    }

    @JsonIgnore
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
