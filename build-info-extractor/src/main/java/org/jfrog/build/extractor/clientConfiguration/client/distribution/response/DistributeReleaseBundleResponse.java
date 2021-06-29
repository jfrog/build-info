package org.jfrog.build.extractor.clientConfiguration.client.distribution.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class DistributeReleaseBundleResponse implements Serializable {
    private static final long serialVersionUID = 1L;

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
