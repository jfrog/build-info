package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Represents a request to sign a release bundle.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class SignReleaseBundleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("storing_repository")
    String storingRepository;

    public SignReleaseBundleRequest() {
    }

    public SignReleaseBundleRequest(String storingRepository) {
        this.storingRepository = storingRepository;
    }

    public String getStoringRepository() {
        return storingRepository;
    }

    public void setStoringRepository(String storingRepository) {
        this.storingRepository = storingRepository;
    }
}
