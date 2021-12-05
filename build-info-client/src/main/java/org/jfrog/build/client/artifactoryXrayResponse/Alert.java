package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Used for serialization of Xray scanning results
 */
public class Alert implements Serializable {
    private final static long serialVersionUID = -4013872536159025854L;
    private String created;
    private List<Issue> issues = null;
    @JsonProperty("top_severity")
    private String topSeverity;
    @JsonProperty("watch_name")
    private String watchName;

    /**
     * No args constructor for use in serialization
     */
    public Alert() {
    }

    public Alert(String created, String topSeverity, String watchName, List<Issue> issues) {
        this.created = created;
        this.topSeverity = topSeverity;
        this.watchName = watchName;
        this.issues = issues;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    @JsonProperty("top_severity")
    public String getTopSeverity() {
        return topSeverity;
    }

    @JsonProperty("top_severity")
    public void setTopSeverity(String topSeverity) {
        this.topSeverity = topSeverity;
    }

    @JsonProperty("watch_name")
    public String getWatchName() {
        return watchName;
    }

    @JsonProperty("watch_name")
    public void setWatchName(String watchName) {
        this.watchName = watchName;
    }

    public List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(List<Issue> issues) {
        this.issues = issues;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
