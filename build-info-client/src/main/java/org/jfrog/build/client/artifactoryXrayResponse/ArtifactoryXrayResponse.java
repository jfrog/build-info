package org.jfrog.build.client.artifactoryXrayResponse;

import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Used for serialization of Xray scanning results
 */
public class ArtifactoryXrayResponse implements Serializable {
    private final static long serialVersionUID = -1632171048760650595L;
    private Summary summary;
    private List<Alert> alerts = null;
    private List<License> licenses = null;

    /**
     * No args constructor for use in serialization
     */
    public ArtifactoryXrayResponse() {
    }

    public ArtifactoryXrayResponse(Summary summary, List<Alert> alerts, List<License> licenses) {
        this.summary = summary;
        this.alerts = alerts;
        this.licenses = licenses;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<License> getLicenses() {
        return licenses;
    }

    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
