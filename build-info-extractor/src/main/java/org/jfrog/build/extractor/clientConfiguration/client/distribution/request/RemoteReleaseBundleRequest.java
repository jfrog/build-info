package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.DistributionRules;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a request to distribute/delete a release bundle.
 *
 * @author yahavi
 */
public abstract class RemoteReleaseBundleRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    @JsonProperty("distribution_rules")
    List<DistributionRules> distributionRules;
    @JsonProperty("dry_run")
    boolean dryRun;

    @SuppressWarnings("unused")
    public List<DistributionRules> getDistributionRules() {
        return distributionRules;
    }

    public void setDistributionRules(List<DistributionRules> distributionRules) {
        this.distributionRules = distributionRules;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
