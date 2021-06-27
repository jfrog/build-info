package org.jfrog.build.extractor.clientConfiguration.client.distribution.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.DistributionRules;

import java.util.List;

/**
 * @author yahavi
 */
public class DistributeReleaseBundleRequest {
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
