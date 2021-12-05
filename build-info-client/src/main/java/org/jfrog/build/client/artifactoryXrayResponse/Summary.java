package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Used for serialization of Xray scanning results
 */
public class Summary implements Serializable {
    private final static long serialVersionUID = -1373752199883531450L;
    private String message;
    @JsonProperty("total_alerts")
    private int totalAlerts;
    @JsonProperty("fail_build")
    private boolean failBuild;
    @JsonProperty("more_details_url")
    private String moreDetailsUrl;

    /**
     * No args constructor for use in serialization
     */
    public Summary() {
    }

    public Summary(int totalAlerts, boolean failBuild, String message, String moreDetailsUrl) {
        this.totalAlerts = totalAlerts;
        this.failBuild = failBuild;
        this.message = message;
        this.moreDetailsUrl = moreDetailsUrl;
    }

    @JsonProperty("total_alerts")
    public int getTotalAlerts() {
        return totalAlerts;
    }

    @JsonProperty("total_alerts")
    public void setTotalAlerts(int totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    @JsonProperty("fail_build")
    public boolean isFailBuild() {
        return failBuild;
    }

    @JsonProperty("fail_build")
    public void setFailBuild(boolean failBuild) {
        this.failBuild = failBuild;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("more_details_url")
    public String getMoreDetailsUrl() {
        return moreDetailsUrl;
    }

    @JsonProperty("more_details_url")
    public void setMoreDetailsUrl(String moreDetailsUrl) {
        this.moreDetailsUrl = moreDetailsUrl;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
