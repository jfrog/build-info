package org.artifactory.build.client;

import org.artifactory.build.api.Build;
import org.artifactory.build.api.builder.BuildInfoBuilder;
import org.testng.annotations.Test;

import java.io.File;
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

    //private String artifactoryUrl = "http://repo.jfrog.org/artifactory";
    private String artifactoryUrl = "http://localhost:8081/artifactory";

    public void getLocalRepositoriesKeys() throws IOException {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl);
        List<String> repositoryKeys = client.getLocalRepositoriesKeys();
        assertNotNull(repositoryKeys, "Repositories keys should not be null");
        assertTrue(repositoryKeys.size() > 0, "Expected to get some repositories");
    }

    @Test(enabled = false, expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".* Unauthorized")
    public void postBuildInfoWithBadCredentials() throws IOException {
        Build build = new Build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "no-such-user", "test");

        client.sendBuildInfo(build);
    }

    public void postBuildInfo() throws IOException {
        Build build = new BuildInfoBuilder("build").started("test").build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password");
        client.sendBuildInfo(build);
    }

    public void deployFile() throws IOException {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password");
        for (int i = 0; i < 10; i++) {
            String version = "1." + i;
            DeployDetails details = new DeployDetails.Builder().targetRepository("libs-releases-local")
                    .artifactPath(String.format("/test/test/%s/test-%s.pom", version, version))
                    .file(new File("build-info-client/pom.xml"))
                    .addProperty("key" + i, "value " + i)
                    .addProperty("key" + i + "b", "value " + i + "b").build();
            client.deployArtifact(details);
        }
    }
}
