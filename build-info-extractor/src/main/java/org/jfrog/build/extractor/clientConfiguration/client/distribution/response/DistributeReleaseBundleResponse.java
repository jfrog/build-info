package org.jfrog.build.extractor.clientConfiguration.client.distribution.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class DistributeReleaseBundleResponse {
    private List<DistributionStatusResponse.TargetArtifactory> sites;
    @JsonProperty("id")
    private String trackerId;

    public List<DistributionStatusResponse.TargetArtifactory> getSites() {
        return sites;
    }

    public void setSites(List<DistributionStatusResponse.TargetArtifactory> sites) {
        this.sites = sites;
    }

    public String getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(String trackerId) {
        this.trackerId = trackerId;
    }
}
