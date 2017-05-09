package org.jfrog.build.api.builder;

import org.jfrog.build.api.release.Distribution;

import java.util.List;

/**
 * Created by yahavi on 12/04/2017.
 */
public class DistributionBuilder {
    private boolean publish = true;
    private boolean overrideExistingFiles = false;
    private String gpgPassphrase;
    private boolean async = false;
    private String targetRepo;
    private List<String> sourceRepos;
    private boolean dryRun = false;

    public DistributionBuilder publish(boolean publish) {
        this.publish = publish;
        return this;
    }

    public DistributionBuilder overrideExistingFiles(boolean overrideExistingFiles) {
        this.overrideExistingFiles = overrideExistingFiles;
        return this;
    }

    public DistributionBuilder gpgPassphrase(String gpgPassphrase) {
        this.gpgPassphrase = gpgPassphrase;
        return this;
    }

    public DistributionBuilder async(boolean async) {
        this.async = async;
        return this;
    }

    public DistributionBuilder targetRepo(String targetRepo) {
        this.targetRepo = targetRepo;
        return this;
    }

    public DistributionBuilder sourceRepos(List<String> sourceRepos) {
        this.sourceRepos = sourceRepos;
        return this;
    }

    public DistributionBuilder dryRun(boolean dryRun) {
        this.dryRun = dryRun;
        return this;
    }

    public Distribution build() {
        return new Distribution(publish, overrideExistingFiles, gpgPassphrase, async, targetRepo, sourceRepos, dryRun);
    }
}
