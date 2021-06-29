package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a request to delete a remote release bundle.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class DeleteReleaseBundleRequest extends RemoteReleaseBundleRequest {
    public enum OnSuccess {
        keep, delete
    }

    @JsonProperty("on_success")
    private OnSuccess onSuccess;

    public OnSuccess getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(OnSuccess onSuccess) {
        this.onSuccess = onSuccess;
    }
}
