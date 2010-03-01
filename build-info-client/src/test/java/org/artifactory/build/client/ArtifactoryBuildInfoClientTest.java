package org.artifactory.build.client;

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration test for {@link ArtifactoryBuildInfoClient}
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryBuildInfoClientTest {

    public void getLocalRepositoriesKeys() {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(
                "http://repo.jfrog.org/artifactory");
        List<String> repositoryKeys = client.getLocalRepositoriesKeys();
        assertNotNull(repositoryKeys, "Repositories keys should not be null");
        assertTrue(repositoryKeys.size() > 0, "Expected to get some repositories");
    }

}
