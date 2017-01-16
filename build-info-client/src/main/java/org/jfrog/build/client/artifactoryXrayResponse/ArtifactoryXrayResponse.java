package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed while parsing JSON response", e);
        }
    }
}
