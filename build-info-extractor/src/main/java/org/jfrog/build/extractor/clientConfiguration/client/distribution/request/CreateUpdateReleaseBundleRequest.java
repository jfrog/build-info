package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseBundleSpec;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseNotes;
import org.jfrog.filespecs.FileSpec;

import java.io.IOException;
import java.io.Serializable;

/**
 * Represents a request to update/create a release bundle.
 *
 * @author yahavi
 */
@SuppressWarnings("unused")
public abstract class CreateUpdateReleaseBundleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("release_notes")
    private ReleaseNotes releaseNotes;
    @JsonProperty("sign_immediately")
    private boolean signImmediately;
    private ReleaseBundleSpec spec;
    private String description;
    @JsonProperty("storing_repository")
    private String storingRepository;
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

    public String getStoringRepository() {
        return storingRepository;
    }

    public void setStoringRepository(String storingRepository) {
        this.storingRepository = storingRepository;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public static abstract class Builder<T extends CreateUpdateReleaseBundleRequest, B extends Builder<T, B>> {
        private ReleaseNotes releaseNotes;
        private String storingRepository;
        private boolean signImmediately;
        private ReleaseBundleSpec spec;
        private String description;
        private boolean dryRun;

        @SuppressWarnings("unchecked")
        private B self() {
            return (B) this;
        }

        public B releaseNotes(ReleaseNotes releaseNotes) {
            this.releaseNotes = releaseNotes;
            return self();
        }

        public B storingRepository(String storingRepository) {
            this.storingRepository = storingRepository;
            return self();
        }

        public B signImmediately(boolean signImmediately) {
            this.signImmediately = signImmediately;
            return self();
        }

        public B spec(String spec) throws IOException {
            this.spec = Utils.createReleaseBundleSpec(spec);
            return self();
        }

        public B spec(FileSpec spec) throws IOException {
            this.spec = Utils.createReleaseBundleSpec(spec);
            return self();
        }

        public B description(String description) {
            this.description = description;
            return self();
        }

        public B dryRun(boolean dryRun) {
            this.dryRun = dryRun;
            return self();
        }

        public abstract T build();

        T build(T request) {
            request.setReleaseNotes(releaseNotes);
            request.setStoringRepository(storingRepository);
            request.setSignImmediately(signImmediately);
            request.setDescription(description);
            request.setDryRun(dryRun);
            request.setSpec(spec);
            return request;
        }
    }
}
