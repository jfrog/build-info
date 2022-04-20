package org.jfrog.build.extractor.docker.extractor;

import org.jfrog.build.extractor.docker.DockerUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.testng.Assert.assertEqualsNoOrder;

@Test
public class DockerUtilsTest {
    @Test
    public void getArtManifestPathTest() {
        String imagePath = "hello-world:latest";
        String repository = "docker-local";
        DockerUtils.CommandType cmdType = DockerUtils.CommandType.Push;
        List<String> results = DockerUtils.getArtManifestPath(imagePath, repository, cmdType);
        assertEqualsNoOrder(results.toArray(), Stream.of("docker-local/hello-world:latest", "hello-world:latest").toArray());

        cmdType = DockerUtils.CommandType.Pull;
        imagePath = "docker-local/hello-world:latest";
        results = DockerUtils.getArtManifestPath(imagePath, repository, cmdType);
        assertEqualsNoOrder(results.toArray(), Stream.of("docker-local/docker-local/hello-world:latest", "docker-local/hello-world:latest", "docker-local/library/docker-local/hello-world:latest", "docker-local/library/hello-world:latest").toArray());
    }
}
