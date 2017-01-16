package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Used for serialization of Xray scanning results
 */
public class Issue implements Serializable {
    private final static long serialVersionUID = -2567386345962539129L;
    private String severity;
    private String type;
    private String provider;
    private String created;
    private String summary;
    private String description;
    private String cve;
    @JsonProperty("impacted_artifacts")
    private List<ImpactedArtifact> impactedArtifacts = null;

    /**
     * No args constructor for use in serialization
     */
    public Issue() {
    }

    public Issue(String severity, String type, String provider, String created, String summary, String description, List<ImpactedArtifact> impactedArtifacts, String cve) {
        this.severity = severity;
        this.type = type;
        this.provider = provider;
        this.created = created;
        this.summary = summary;
        this.description = description;
        this.impactedArtifacts = impactedArtifacts;
        this.cve = cve;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @JsonProperty("impacted_artifacts")
    public List<ImpactedArtifact> getImpactedArtifacts() {
        return impactedArtifacts;
    }

    @JsonProperty("impacted_artifacts")
    public void setImpactedArtifacts(List<ImpactedArtifact> impactedArtifacts) {
        this.impactedArtifacts = impactedArtifacts;
    }

    public String getCve() {
        return cve;
    }

    public void setCve(String cve) {
        this.cve = cve;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
