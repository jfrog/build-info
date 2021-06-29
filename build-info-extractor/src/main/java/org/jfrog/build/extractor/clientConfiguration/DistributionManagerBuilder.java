package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.extractor.clientConfiguration.client.distribution.DistributionManager;

public class DistributionManagerBuilder extends ManagerBuilderBase<DistributionManagerBuilder> {

    @Override
    public DistributionManager build() {
        DistributionManager distributionManager = new DistributionManager(serverUrl, username, password, accessToken, log);
        build(distributionManager);
        return distributionManager;
    }

    @Override
    protected DistributionManagerBuilder self() {
        return this;
    }
}
