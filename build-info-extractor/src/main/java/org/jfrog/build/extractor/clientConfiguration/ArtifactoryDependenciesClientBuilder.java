package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class ArtifactoryDependenciesClientBuilder extends ArtifactoryClientBuilderBase<ArtifactoryDependenciesClientBuilder> {

    @Override
    public ArtifactoryDependenciesClient build() {
        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(artifactoryUrl, username, password, log);
        build(client);
        return client;
    }

    @Override
    protected ArtifactoryDependenciesClientBuilder self() {
        return this;
    }
}
