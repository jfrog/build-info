package org.jfrog.build.extractor.clientConfiguration;

import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

/**
 * Created by Bar Belity on 10/10/2018.
 */
public class ArtifactoryManagerBuilder extends ManagerBuilderBase<ArtifactoryManagerBuilder> {

    @Override
    public ArtifactoryManager build() {
        ArtifactoryManager client = new ArtifactoryManager(serverUrl, username, password, accessToken, log);
        build(client);
        return client;
    }

    @Override
    protected ArtifactoryManagerBuilder self() {
        return this;
    }
}
