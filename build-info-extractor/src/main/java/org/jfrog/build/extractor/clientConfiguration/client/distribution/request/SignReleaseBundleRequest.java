package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yahavi
 */
public class SignReleaseBundleRequest {
    @JsonProperty("storing_repository")
    String storingRepository;

    public SignReleaseBundleRequest() {
    }

    public SignReleaseBundleRequest(String storingRepository) {
        this.storingRepository = storingRepository;
    }

    @SuppressWarnings("unused")
    public String getStoringRepository() {
        return storingRepository;
    }

    @SuppressWarnings("unused")
    public void setStoringRepository(String storingRepository) {
        this.storingRepository = storingRepository;
    }
}
