package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yahavi
 **/
public class DeleteReleaseBundleRequest extends DistributeReleaseBundleRequest {

    @JsonProperty("on_success")
    private OnSuccess onSuccess;

    @SuppressWarnings("unused")
    public OnSuccess getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(OnSuccess onSuccess) {
        this.onSuccess = onSuccess;
    }

    public enum OnSuccess {
        keep, delete
    }
}
