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
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.testng.Assert.*;

@Test
public class DockerExtractorTest extends IntegrationTestsBase {
    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));
    private static final String SHORT_IMAGE_NAME = "jfrog_artifactory_buildinfo_tests";
    private static final String SHORT_IMAGE_TAG = "2";
    private static final String EXPECTED_REMOTE_PATH = SHORT_IMAGE_NAME + "/" + SHORT_IMAGE_TAG;

    private static final String LOCAL_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_LOCAL_DOMAIN";
    private static final String REMOTE_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_REMOTE_DOMAIN";
    private static final String VIRTUAL_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_DOMAIN";
    private static final String DOCKER_LOCAL_REPO = "BITESTS_ARTIFACTORY_DOCKER_LOCAL_REPO";
    private static final String DOCKER_REMOTE_REPO = "BITESTS_ARTIFACTORY_DOCKER_REMOTE_REPO";
    private static final String DOCKER_VIRTUAL_REPO = "BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_REPO";
    private static final String DOCKER_HOST = "BITESTS_ARTIFACTORY_DOCKER_HOST";
    private final ArrayListMultimap<String, String> artifactProperties = ArrayListMultimap.create();
    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private String localDomainName;
    private String remoteDomainName;
    private String pullImageFromVirtual;
    private String virtualDomainName;
    private String dockerLocalRepo;
    private String dockerRemoteRepo;
    private String dockerVirtualRepo;
    private String host;
    private String imageTag;
    private String pushImageTag;
    private String pullImageFromRemote;

    public DockerExtractorTest() {
        localRepo = "";
        virtualRepo = "";
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
        localDomainName = validateDomainSuffix(System.getenv(LOCAL_DOMAIN));
        remoteDomainName = validateDomainSuffix(System.getenv(REMOTE_DOMAIN));
        virtualDomainName = validateDomainSuffix(System.getenv(VIRTUAL_DOMAIN));
        dockerLocalRepo = System.getenv(DOCKER_LOCAL_REPO);
        dockerRemoteRepo = System.getenv(DOCKER_REMOTE_REPO);
        dockerVirtualRepo = System.getenv(DOCKER_VIRTUAL_REPO);
        host = System.getenv(DOCKER_HOST);
        imageTag = domainName + SHORT_IMAGE_NAME + ":" + SHORT_IMAGE_TAG;
        pushImageTag = localDomainName + "jfrog_artifactory_buildinfo_tests:2";
        pullImageFromRemote = remoteDomainName + "hello-world:latest";
        pullImageFromVirtual = virtualDomainName + "hello-world:latest";
    }

    @Test
    public void dockerPushTest() {
        if (isWindows()) {
            throw new SkipException("Skipping Docker tests on Windows OS");
        }
        try {
            if (StringUtils.isBlank(localDomainName)) {
                throw new IOException("The " + LOCAL_DOMAIN + " environment variable is not set, failing docker tests.");
            }
            if (StringUtils.isBlank(dockerLocalRepo)) {
                throw new IOException("The " + DOCKER_LOCAL_REPO + " environment variable is not set, failing docker tests.");
            }
            String projectPath = PROJECTS_ROOT.resolve("docker-push").toAbsolutePath().toString();
            DockerJavaWrapper.buildImage(pushImageTag, host, Collections.emptyMap(), projectPath);

            DockerPush dockerPush = new DockerPush(buildInfoClientBuilder, dependenciesClientBuilder, pushImageTag, host, artifactProperties, dockerLocalRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
            Build build = dockerPush.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);

            assertEquals(module.getType(), "docker");
            assertEquals(module.getRepository(), repo);
            assertEquals(7, module.getArtifacts().size());
            module.getArtifacts().forEach(artifact -> assertEquals(artifact.getRemotePath(), EXPECTED_REMOTE_PATH));
            assertEquals(5, module.getDependencies().size());
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @Test
    public void dockerPullFromRemoteTest() {
        if (isWindows()) {
            getLog().info("Skipping Docker tests on Windows OS");
            return;
        }
        try {
            if (StringUtils.isBlank(remoteDomainName)) {
                throw new IOException("The " + REMOTE_DOMAIN + " environment variable is not set, failing docker tests.");
            }
            if (StringUtils.isBlank(dockerRemoteRepo)) {
                throw new IOException("The " + DOCKER_REMOTE_REPO + " environment variable is not set, failing docker tests.");
            }
            DockerPull dockerPull = new DockerPull(buildInfoClientBuilder, dependenciesClientBuilder, pullImageFromRemote, host, dockerRemoteRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
            validatePulledBuild(dockerPull.execute());
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @Test
    public void dockerPullFromVirtualTest() {
        if (isWindows()) {
            getLog().info("Skipping Docker tests on Windows OS");
            return;
        }
        try {
            if (StringUtils.isBlank(virtualDomainName)) {
                throw new IOException("The " + VIRTUAL_DOMAIN + " environment variable is not set, failing docker tests.");
            }
            if (StringUtils.isBlank(dockerVirtualRepo)) {
                throw new IOException("The " + DOCKER_VIRTUAL_REPO + " environment variable is not set, failing docker tests.");
            }
            DockerPull dockerPull = new DockerPull(buildInfoClientBuilder, dependenciesClientBuilder, pullImageFromVirtual, host, dockerVirtualRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
            validatePulledBuild(dockerPull.execute());
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    private void validatePulledBuild(Build build) {
        assertEquals(build.getModules().size(), 1);
        Module module = build.getModules().get(0);

        assertEquals(null, module.getArtifacts());
        // Latest tag may change the number of dependencies in the future.
        assertTrue(module.getDependencies().size() > 0);
    }

    private String validateDomainSuffix(String domain) {
        if (!StringUtils.endsWith(domain, "/")) {
            domain += "/";
        }
        return domain;
    }

    @AfterClass
    private void tearDown() throws IOException {
        deleteContentFromRepo(dockerLocalRepo);
    }
}