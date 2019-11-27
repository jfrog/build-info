package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

/**
 * @author Yahav Itzhak
 */
public class ArtifactoryDependenciesClientBuilder extends ArtifactoryClientBuilderBase<ArtifactoryDependenciesClientBuilder> {

    @Override
    public ArtifactoryDependenciesClient build() {
        ArtifactoryDependenciesClient client = new ArtifactoryDependenciesClient(artifactoryUrl, username, password, accessToken, log);
        build(client);
        return client;
    }

    @Override
    protected ArtifactoryDependenciesClientBuilder self() {
        return this;
    }
}
