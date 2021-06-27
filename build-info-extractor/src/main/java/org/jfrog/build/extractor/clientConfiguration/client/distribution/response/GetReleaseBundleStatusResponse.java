package org.jfrog.build.extractor.clientConfiguration.client.distribution.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseBundleSpec;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseNotes;

/**
 * @author yahavi
 */
@SuppressWarnings("unused")
public class GetReleaseBundleStatusResponse {
    enum DistributionState {
        // Release bundle created and open for changes:
        OPEN,
        // Release bundle is signed, but not stored:
        SIGNED,
        // Release bundle is signed and stored, but not scanned by Xray:
        STORED,
        // Release bundle is signed, stored and scanned by Xray:
        READY_FOR_DISTRIBUTION
    }

    @JsonProperty("release_notes")
    private ReleaseNotes releaseNotes;
    private DistributionState state;
    private ReleaseBundleSpec spec;
    private String description;
    private String version;
    private String name;

    public ReleaseNotes getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(ReleaseNotes releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public DistributionState getState() {
        return state;
    }

    public void setState(DistributionState state) {
        this.state = state;
    }

    public ReleaseBundleSpec getSpec() {
        return spec;
    }

    public void setSpec(ReleaseBundleSpec spec) {
        this.spec = spec;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
