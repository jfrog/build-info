package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

/**
 * Created by Bar Belity on 10/10/2018.
 */
public class ArtifactoryBuildInfoClientBuilder extends ArtifactoryClientBuilderBase<ArtifactoryBuildInfoClientBuilder> {

    @Override
    public ArtifactoryBuildInfoClient build() {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, username, password, log);
        build(client);
        return client;
    }

    @Override
    protected ArtifactoryBuildInfoClientBuilder self() {
        return this;
    }
}
