package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseBundleSpec;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseNotes;
import org.jfrog.filespecs.FileSpec;

import java.io.IOException;

/**
 * Represents a request to update/create a release bundle for Distribution.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public class UpdateReleaseBundleRequest extends SignReleaseBundleRequest {
    @JsonProperty("release_notes")
    private ReleaseNotes releaseNotes;
    @JsonProperty("sign_immediately")
    private boolean signImmediately;
    private ReleaseBundleSpec spec;
    private String description;
    @JsonProperty("dry_run")
    private boolean dryRun;

    public ReleaseNotes getReleaseNotes() {
        return releaseNotes;
    }

    public void setReleaseNotes(ReleaseNotes releaseNotes) {
        this.releaseNotes = releaseNotes;
    }

    public boolean isSignImmediately() {
        return signImmediately;
    }

    public void setSignImmediately(boolean signImmediately) {
        this.signImmediately = signImmediately;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public ReleaseBundleSpec getSpec() {
        return spec;
    }

    public void setSpec(ReleaseBundleSpec spec) {
        this.spec = spec;
    }

    public static class Builder<T extends UpdateReleaseBundleRequest> {
        private ReleaseNotes releaseNotes;
        private String storingRepository;
        private boolean signImmediately;
        private ReleaseBundleSpec spec;
        private String description;
        private boolean dryRun;

        public Builder<T> releaseNotes(ReleaseNotes releaseNotes) {
            this.releaseNotes = releaseNotes;
            return this;
        }

        public Builder<T> storingRepository(String storingRepository) {
            this.storingRepository = storingRepository;
            return this;
        }

        public Builder<T> signImmediately(boolean signImmediately) {
            this.signImmediately = signImmediately;
            return this;
        }

        public Builder<T> spec(String spec) throws IOException {
            this.spec = Utils.createSpec(spec);
            return this;
        }

        public Builder<T> spec(FileSpec spec) throws IOException {
            this.spec = Utils.createSpec(spec);
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return this;
        }

        @SuppressWarnings("unchecked")
        public T build() throws IOException {
            return build((T) new UpdateReleaseBundleRequest());
        }

        public T build(T releaseBundleRequest) throws IOException {
            releaseBundleRequest.setReleaseNotes(releaseNotes);
            releaseBundleRequest.setStoringRepository(storingRepository);
            releaseBundleRequest.setSignImmediately(signImmediately);
            releaseBundleRequest.setDescription(description);
            releaseBundleRequest.setDryRun(dryRun);
            releaseBundleRequest.setSpec(spec);
            return releaseBundleRequest;
        }
    }
}
