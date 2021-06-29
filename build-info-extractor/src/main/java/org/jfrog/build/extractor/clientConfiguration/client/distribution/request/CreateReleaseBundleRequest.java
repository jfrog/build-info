package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

/**
 * Represents a request to create a release bundle.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class CreateReleaseBundleRequest extends CreateUpdateReleaseBundleRequest {
    private String version;
    private String name;

    @SuppressWarnings("unused")
    public CreateReleaseBundleRequest() {
    }

    public CreateReleaseBundleRequest(String name, String version) {
        this.name = name;
        this.version = version;
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

    public static class Builder extends CreateUpdateReleaseBundleRequest.Builder<CreateReleaseBundleRequest, Builder> {
        private final String version;
        private final String name;

        public Builder(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public CreateReleaseBundleRequest build() {
            CreateReleaseBundleRequest request = build(new CreateReleaseBundleRequest());
            request.setName(name);
            request.setVersion(version);
            return request;
        }
    }
}
