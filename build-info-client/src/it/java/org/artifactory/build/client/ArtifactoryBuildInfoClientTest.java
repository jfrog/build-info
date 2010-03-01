package org.artifactory.build.client;

import org.artifactory.build.api.Build;
import org.artifactory.build.api.builder.BuildInfoBuilder;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration test for {@link ArtifactoryBuildInfoClient}.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryBuildInfoClientTest {

    private String artifactoryUrl = "http://repo.jfrog.org/artifactory";

    public void getLocalRepositoriesKeys() {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl);
        List<String> repositoryKeys = client.getLocalRepositoriesKeys();
        assertNotNull(repositoryKeys, "Repositories keys should not be null");
        assertTrue(repositoryKeys.size() > 0, "Expected to get some repositories");
    }

    @Test(enabled = false, expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".* Unauthorized")
    public void postBuildInfoWithBadCredentials() throws IOException {
        Build build = new Build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "nosuchuser", "test");

        client.sendBuildInfo(build);
    }

    public void postBuildInfo() throws IOException {
        Build build = new BuildInfoBuilder("build").started("test").build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password");
        client.sendBuildInfo(build);
    }
}
