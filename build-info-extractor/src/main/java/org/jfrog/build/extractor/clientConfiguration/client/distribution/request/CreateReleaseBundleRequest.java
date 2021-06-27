package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import java.io.IOException;

/**
 * @author yahavi
 */
public class CreateReleaseBundleRequest extends UpdateReleaseBundleRequest {
    private String version;
    private String name;

    @SuppressWarnings("unused")
    public CreateReleaseBundleRequest() {
    }

    public CreateReleaseBundleRequest(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public static class Builder extends UpdateReleaseBundleRequest.Builder<CreateReleaseBundleRequest> {
        private final String name;
        private final String version;

        public Builder(String name, String version) {
            this.name = name;
            this.version = version;
        }

        @Override
        public CreateReleaseBundleRequest build() throws IOException {
            return build(new CreateReleaseBundleRequest(name, version));
        }
    }
}
