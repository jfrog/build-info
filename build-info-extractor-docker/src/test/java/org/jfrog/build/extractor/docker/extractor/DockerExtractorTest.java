package org.jfrog.build.extractor.docker.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.*;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

@Test
public class DockerExtractorTest extends IntegrationTestsBase {
    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor", "docker"));
    private static final String PROJECT_PATH = PROJECTS_ROOT.toAbsolutePath().toString();
    private static final String SHORT_IMAGE_NAME = "jfrog_artifactory_buildinfo_tests";
    private static final String SHORT_IMAGE_TAG_LOCAL = "2";
    private static final String SHORT_IMAGE_TAG_VIRTUAL = "3";
    private static final String EXPECTED_REMOTE_PATH_KANIKO = "hello-world/latest";

    private static final String LOCAL_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_LOCAL_DOMAIN";
    private static final String REMOTE_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_REMOTE_DOMAIN";
    private static final String VIRTUAL_DOMAIN = "BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_DOMAIN";
    private static final String DOCKER_LOCAL_REPO = "BITESTS_ARTIFACTORY_DOCKER_LOCAL_REPO";
    private static final String DOCKER_REMOTE_REPO = "BITESTS_ARTIFACTORY_DOCKER_REMOTE_REPO";
    private static final String DOCKER_VIRTUAL_REPO = "BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_REPO";
    private static final String DOCKER_HOST = "BITESTS_ARTIFACTORY_DOCKER_HOST";
    private final ArrayListMultimap<String, String> artifactProperties = ArrayListMultimap.create();
    private String pullImageFromVirtual;
    private String virtualDomainName;
    private String dockerLocalRepo;
    private String dockerRemoteRepo;
    private String dockerVirtualRepo;
    private String host;
    private String imageTagLocal;
    private String imageTagVirtual;
    private String pullImageFromRemote;

    public DockerExtractorTest() {
        localRepo1 = "";
        virtualRepo = "";
        artifactProperties.putAll(ImmutableMultimap.<String, String>builder()
                .put("build.name", "docker-push-test")
                .put("build.number", "1")
                .put("build.timestamp", "321")
                .put("property-key", "property-value")
                .build());
    }

    @BeforeClass
    private void setUp() {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw new SkipException("Skipping Docker tests on Windows OS");
        }
        assertEnvironment();
        String localDomainName = StringUtils.appendIfMissing(System.getenv(LOCAL_DOMAIN), "/");
        String remoteDomainName = StringUtils.appendIfMissing(System.getenv(REMOTE_DOMAIN), "/");
        virtualDomainName = System.getenv(VIRTUAL_DOMAIN);
        dockerLocalRepo = System.getenv(DOCKER_LOCAL_REPO);
        dockerRemoteRepo = System.getenv(DOCKER_REMOTE_REPO);
        dockerVirtualRepo = System.getenv(DOCKER_VIRTUAL_REPO);
        host = System.getenv(DOCKER_HOST);
        imageTagLocal = localDomainName + SHORT_IMAGE_NAME + ":" + SHORT_IMAGE_TAG_LOCAL;
        imageTagVirtual = StringUtils.appendIfMissing(virtualDomainName, "/") + SHORT_IMAGE_NAME + ":" + SHORT_IMAGE_TAG_VIRTUAL;
        pullImageFromRemote = remoteDomainName + "hello-world:latest";
        pullImageFromVirtual = StringUtils.appendIfMissing(virtualDomainName, "/") + "hello-world:latest";
    }

    private void assertEnvironment() {
        Lists.newArrayList(LOCAL_DOMAIN, DOCKER_LOCAL_REPO,
                REMOTE_DOMAIN, DOCKER_REMOTE_REPO,
                VIRTUAL_DOMAIN, DOCKER_VIRTUAL_REPO)
                .forEach(envKey -> assertNotNull(System.getenv(envKey), "The '" + envKey + "' environment variable is not set, failing docker tests."));
    }

    @AfterClass
    private void tearDown() throws IOException {
        deleteContentFromRepo(dockerLocalRepo);
    }

    @Test
    public void dockerPushToLocalTest() {
        DockerJavaWrapper.buildImage(imageTagLocal, host, Collections.emptyMap(), PROJECT_PATH);
        DockerPush dockerPush = new DockerPush(artifactoryManagerBuilder, imageTagLocal, host, artifactProperties, dockerLocalRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
        pushAndValidateImage(dockerPush, dockerLocalRepo, imageTagLocal, SHORT_IMAGE_TAG_LOCAL);
    }

    @Test
    public void dockerPushToVirtualTest() {
        DockerJavaWrapper.buildImage(imageTagVirtual, host, Collections.emptyMap(), PROJECT_PATH);
        DockerPush dockerPush = new DockerPush(artifactoryManagerBuilder, imageTagVirtual, host, artifactProperties, dockerVirtualRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
        pushAndValidateImage(dockerPush, dockerVirtualRepo, imageTagVirtual, SHORT_IMAGE_TAG_VIRTUAL);
    }

    @Test
    public void dockerPullFromRemoteTest() {
        DockerPull dockerPull = new DockerPull(artifactoryManagerBuilder, pullImageFromRemote, host, dockerRemoteRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
        validatePulledDockerImage(dockerPull.execute(), pullImageFromRemote);
    }

    @Test
    public void dockerPullFromVirtualTest() {
        DockerPull dockerPull = new DockerPull(artifactoryManagerBuilder, pullImageFromVirtual, host, dockerVirtualRepo, getUsername(), getPassword(), getLog(), Collections.emptyMap());
        validatePulledDockerImage(dockerPull.execute(), pullImageFromVirtual);
    }

    @Test
    public void buildDockerCreateKanikoTest() throws IOException, InterruptedException {
        Path workingDirectory = Files.createTempDirectory("build-docker-create-kaniko-test",
                PosixFilePermissions.asFileAttribute(Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)));
        try {
            FileUtils.copyDirectory(new File(PROJECT_PATH), workingDirectory.toFile());
            Path kanikoConfig = createKanikoConfig(workingDirectory, virtualDomainName);
            String kanikoFile = execKaniko(workingDirectory, virtualDomainName, kanikoConfig);

            BuildDockerCreator buildDockerCreator = new BuildDockerCreator(artifactoryManagerBuilder, kanikoFile, BuildDockerCreator.ImageFileType.KANIKO, artifactProperties, dockerVirtualRepo, getLog());
            Build build = buildDockerCreator.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = getAndValidateModule(build, "hello-world:latest", dockerVirtualRepo);
            module.getArtifacts().stream().map(BaseBuildFileBean::getRemotePath).forEach(remotePath -> assertEquals(remotePath, EXPECTED_REMOTE_PATH_KANIKO));
        } finally {
            FileUtils.deleteDirectory(workingDirectory.toFile());
        }
    }

    @Test
    public void buildDockerCreateJibTest() throws IOException, InterruptedException {
        Path wd = Files.createTempDirectory("build-docker-create-jib-test",
                PosixFilePermissions.asFileAttribute(Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)));
        try {
            FileUtils.copyDirectory(PROJECTS_ROOT.resolve("maven-jib-example").toFile(), wd.toFile());
            execJib(wd);

            // Run build-docker-create
            BuildDockerCreator.ImageFileType imageFileType = BuildDockerCreator.ImageFileType.JIB;
            Build build = new BuildDockerCreator(artifactoryManagerBuilder, getJibImageJsonPath(wd),
                    imageFileType, artifactProperties, dockerVirtualRepo, getLog()).execute();

            // Check modules
            assertEquals(build.getModules().size(), 3);
            Module module = getAndValidateModule(build, "multi1", dockerVirtualRepo);
            assertFalse(module.getArtifacts().isEmpty());
            module = getAndValidateModule(build, "multi2", dockerVirtualRepo);
            assertFalse(module.getArtifacts().isEmpty());
            module = getAndValidateModule(build, "multi3", dockerVirtualRepo);
            assertFalse(module.getArtifacts().isEmpty());
        } finally {
            FileUtils.deleteDirectory(wd.toFile());
        }
    }

    private Path createKanikoConfig(Path workingDirectory, String registry) throws IOException {
        Path kanikoConfigPath = workingDirectory.toAbsolutePath().resolve("kaniko-config.json");
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode credentials = mapper.createObjectNode()
                .put("username", getUsername())
                .put("password", getPassword());
        ObjectNode registryNode = mapper.createObjectNode().set(registry, credentials);
        String kanikoConfig = mapper.createObjectNode().set("auths", registryNode).toPrettyString();
        Files.write(kanikoConfigPath, kanikoConfig.getBytes(StandardCharsets.UTF_8));
        return kanikoConfigPath;
    }

    private String execKaniko(Path workingDirectory, String registry, Path kanikoConfigPath) throws IOException, InterruptedException {
        CommandExecutor commandExecutor = new CommandExecutor("docker", null);
        List<String> args = Lists.newArrayList("run", "--rm", "-v",
                workingDirectory.toAbsolutePath() + ":/workspace", "-v",
                kanikoConfigPath + ":/kaniko/.docker/config.json:ro", "gcr.io/kaniko-project/executor:latest",
                "--dockerfile=Dockerfile", "--destination=" + registry + "/hello-world",
                "--image-name-tag-with-digest-file=image-file");
        CommandResults results = commandExecutor.exeCommand(workingDirectory.toFile(), args, null, getLog());
        assertTrue(results.isOk(), results.getErr());
        return workingDirectory.resolve("image-file").toAbsolutePath().toString();
    }

    private void execJib(Path workingDirectory) throws IOException, InterruptedException {
        CommandExecutor commandExecutor = new CommandExecutor("mvn", null);
        List<String> args = Lists.newArrayList("compile", "jib:build");
        CommandResults results = commandExecutor.exeCommand(workingDirectory.toFile(), args, null, getLog());
        assertTrue(results.isOk(), results.getErr());
    }

    private String getJibImageJsonPath(Path workingDirectory) {
        return workingDirectory.resolve("*").resolve("target").resolve("jib-image.json").toAbsolutePath().toString();
    }

    private void pushAndValidateImage(DockerPush dockerPush, String repo, String imageTag, String shortImageTag) {
        Build build = dockerPush.execute();
        Module module = getAndValidateModule(build, SHORT_IMAGE_NAME + ":" + shortImageTag, repo);
        List<Artifact> artifacts = module.getArtifacts();
        validateImageArtifacts(artifacts, imageTag);
        assertEquals(7, artifacts.size());
        module.getArtifacts().forEach(artifact -> assertEquals(artifact.getRemotePath(), SHORT_IMAGE_NAME + "/" + shortImageTag));
        assertEquals(5, module.getDependencies().size());
    }

    private void validatePulledDockerImage(Build build, String image) {
        assertEquals(build.getModules().size(), 1);
        Module module = build.getModules().get(0);
        assertEquals(module.getType(), "docker");
        List<Dependency> dependencies = module.getDependencies();
        validateImageDependencies(dependencies, image);
    }

    private void validateImageDependencies(List<Dependency> deps, String image) {
        // Latest tag may change the number of dependencies in the future.
        assertFalse(deps.isEmpty());
        String imageDigest = getImageId(image);
        assertTrue(deps.stream().anyMatch(dep -> dep.getId().equals(imageDigest)));
    }

    private Module getAndValidateModule(Build build, String id, String repo) {
        Module module = build.getModules().stream()
                .filter(moduleCandidate -> StringUtils.equals(moduleCandidate.getId(), id))
                .findFirst().orElse(null);
        assertNotNull(module);
        assertEquals(module.getType(), "docker");
        assertEquals(module.getRepository(), repo);
        return module;
    }

    private void validateImageArtifacts(List<Artifact> arts, String image) {
        String imageDigest = getImageId(image);
        assertTrue(arts.stream().anyMatch(art -> art.getName().equals(imageDigest)));
    }

    private String getImageId(String image) {
        String id = DockerJavaWrapper.InspectImage(image, host, Collections.emptyMap(), getLog()).getId();
        assertNotNull(id);
        return id.replace(":", "__");
    }
}
