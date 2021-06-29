package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

/**
 * Represents a request to update a release bundle.
 *
 * @author yahavi
 */
public class UpdateReleaseBundleRequest extends CreateUpdateReleaseBundleRequest {

    public static class Builder extends CreateUpdateReleaseBundleRequest.Builder<UpdateReleaseBundleRequest, Builder> {
        public UpdateReleaseBundleRequest build() {
            return build(new UpdateReleaseBundleRequest());
        }
    }
}
