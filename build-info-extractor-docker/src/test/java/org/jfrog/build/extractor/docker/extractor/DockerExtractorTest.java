package org.jfrog.build.extractor.docker.extractor;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class DockerExtractorTest extends IntegrationTestsBase {
    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));
    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private String domainName;
    private String repo;
    private String host;
    private String imageTag;
    private ArrayListMultimap<String, String> artifactProperties = ArrayListMultimap.create();

    public DockerExtractorTest() {
        localRepo = "";
        virtualRepo = "";
        artifactProperties.put("build.name", "docker-push-test");
        artifactProperties.putAll(ImmutableMultimap.<String, String>builder()
                .put("build.name", "docker-push-test")
                .put("build.number", "1")
                .put("build.timestamp", "321")
                .put("property-key", "property-value")
                .build());
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    @BeforeClass
    private void setUp() {
        dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        buildInfoClientBuilder = new ArtifactoryBuildInfoClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        // Get image name
        domainName = System.getenv("BITESTS_ARTIFACTORY_DOCKER_DOMAIN");
        repo = System.getenv("BITESTS_ARTIFACTORY_DOCKER_REPO");
        if (!StringUtils.endsWith(domainName, "/")) {
            domainName += "/";
        }
        imageTag = domainName + "jfrog_artifactory_buildinfo_tests:2";
        host = System.getenv("BITESTS_ARTIFACTORY_DOCKER_HOST");
    }

    @Test
    @SuppressWarnings("unused")
    private void dockerPushTest() {
        if (isWindows()) {
            getLog().info("Skipping Docker tests on Windows OS");
            return;
        }
        try {
            if (StringUtils.isBlank(domainName)) {
                throw new IOException("The BITESTS_ARTIFACTORY_DOCKER_DOMAIN environment variable is not set, failing docker tests.");
            }
            if (StringUtils.isBlank(repo)) {
                throw new IOException("The BITESTS_ARTIFACTORY_DOCKER_REPO environment variable is not set, failing docker tests.");
            }
            String projectPath = PROJECTS_ROOT.resolve("docker-push").toAbsolutePath().toString();
            DockerJavaWrapper.buildImage(imageTag, host, Collections.emptyMap(), projectPath);

            DockerPush dockerPush = new DockerPush(buildInfoClientBuilder, dependenciesClientBuilder, imageTag, host, artifactProperties, repo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
            Build build = dockerPush.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);

            assertEquals(7, module.getArtifacts().size());
            assertEquals(5, module.getDependencies().size());
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @AfterClass
    private void tearDown() throws IOException {
        deleteContentFromRepo(repo);
    }
}