package org.jfrog.build.extractor.docker;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.jfrog.build.extractor.docker.DockerUtils.calculateModuleId;
import static org.jfrog.build.extractor.docker.DockerUtils.getArtManifestPath;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

@Test
public class DockerUtilsTest {
    @Test
    public void getArtManifestPathTest() {
        String imagePath = "hello-world:latest";
        String repository = "docker-local";
        DockerUtils.CommandType cmdType = DockerUtils.CommandType.Push;
        List<String> results = getArtManifestPath(imagePath, repository, cmdType);
        assertEqualsNoOrder(results.toArray(), Stream.of("docker-local/hello-world:latest", "hello-world:latest").toArray());

        cmdType = DockerUtils.CommandType.Pull;
        imagePath = "docker-local/hello-world:latest";
        results = getArtManifestPath(imagePath, repository, cmdType);
        assertEqualsNoOrder(results.toArray(), Stream.of("docker-local/docker-local/hello-world:latest", "docker-local/hello-world:latest", "docker-local/library/docker-local/hello-world:latest", "docker-local/library/hello-world:latest").toArray());
    }

    @DataProvider
    private Object[][] moduleIdsProvider() {
        return new String[][]{
                // imageTag, targetRepo, expectedModuleId
                {"acme-docker-local.jfrog.io/image-tag", "docker-repo", "image-tag"},
                {"acme-docker-local.jfrog.io/image-tag:1", "docker-repo", "image-tag:1"},
                {"acme-docker-local.jfrog.io/image/tag", "docker-repo", "image/tag"},
                {"acme-docker-local.jfrog.io/image/tag:1", "docker-repo", "image/tag:1"},
                {"acme-docker-local.jfrog.io/image-tag", "other-docker-repo", "image-tag"},
                {"acme-docker-local.jfrog.io/image-tag:1", "other-docker-repo", "image-tag:1"},
                {"acme-docker-local.jfrog.io/image/tag", "other-docker-repo", "image/tag"},
                {"acme-docker-local.jfrog.io/image/tag:1", "other-docker-repo", "image/tag:1"},

                {"acme.jfrog.io/docker-repo/image-tag", "docker-repo", "image-tag"},
                {"acme.jfrog.io/docker-repo/image-tag:1", "docker-repo", "image-tag:1"},
                {"acme.jfrog.io/docker-repo/image/tag", "docker-repo", "image/tag"},
                {"acme.jfrog.io/docker-repo/image/tag:1", "docker-repo", "image/tag:1"},
                // Edge cases - we have no choice but to add the docker-repo to the module id
                {"acme.jfrog.io/docker-repo/image-tag", "other-docker-repo", "docker-repo/image-tag"},
                {"acme.jfrog.io/docker-repo/image-tag:1", "other-docker-repo", "docker-repo/image-tag:1"},
                {"acme.jfrog.io/docker-repo/image/tag", "other-docker-repo", "docker-repo/image/tag"},
                {"acme.jfrog.io/docker-repo/image/tag:1", "other-docker-repo", "docker-repo/image/tag:1"},
        };
    }

    @Test(dataProvider = "moduleIdsProvider")
    public void calculateModuleIdTest(String imageTag, String targetRepo, String expectedModuleId) {
        assertEquals(calculateModuleId(imageTag, targetRepo), expectedModuleId);
    }
}
