package org.jfrog.build.extractor.docker.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.multiMap.ListMultimap;
import org.jfrog.build.api.multiMap.Multimap;
import org.jfrog.build.extractor.ci.*;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.docker.DockerJavaWrapper;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;

import static org.testng.Assert.*;

@Test
public class DockerExtractorTest extends IntegrationTestsBase {
    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor", "docker"));
    private static final String PROJECT_PATH = PROJECTS_ROOT.toAbsolutePath().toString();
    private static final String DOCKER_LOCAL_REPO = "build-info-tests-docker-local";
    private static final String DOCKER_REMOTE_REPO = "build-info-tests-docker-remote";
    private static final String DOCKER_VIRTUAL_REPO = "build-info-tests-docker-virtual";
    private static final String SHORT_IMAGE_NAME = "jfrog_artifactory_buildinfo_tests";
    private static final String SHORT_IMAGE_TAG_LOCAL = "2";
    private static final String SHORT_IMAGE_TAG_VIRTUAL = "3";
    private static final String EXPECTED_REMOTE_PATH_KANIKO = "hello-world/latest";
    private static final String DOCKER_HOST = "BITESTS_ARTIFACTORY_DOCKER_HOST";
    private final Multimap<String, String> artifactProperties;
    private String pullImageFromVirtual;
    private String virtualDomainName;
    private String host;
    private String imageTagLocal;
    private String imageTagVirtual;
    private String pullImageFromRemote;
    private Map<String, String> envVars;

    public DockerExtractorTest() {
        localRepo1 = getKeyWithTimestamp(DOCKER_LOCAL_REPO);
        remoteRepo = getKeyWithTimestamp(DOCKER_REMOTE_REPO);
        virtualRepo = getKeyWithTimestamp(DOCKER_VIRTUAL_REPO);
        artifactProperties = new ListMultimap<String, String>() {{
            put("build.name", "docker-push-test");
            put("build.number", "1");
            put("build.timestamp", "321");
            put("property-key", "property-value");
        }};
    }

    @BeforeClass
    private void setUp() {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw new SkipException("Skipping Docker tests on Windows OS");
        }
        host = System.getenv(DOCKER_HOST);
        String containerRegistry = readParam(new Properties(), "container_registry", "127.0.0.1:8081");
        virtualDomainName = StringUtils.appendIfMissing(containerRegistry, "/") + virtualRepo;
        imageTagLocal = StringUtils.appendIfMissing(containerRegistry, "/") + localRepo1 + "/" + SHORT_IMAGE_NAME + ":" + SHORT_IMAGE_TAG_LOCAL;
        imageTagVirtual = StringUtils.appendIfMissing(virtualDomainName, "/") + SHORT_IMAGE_NAME + ":" + SHORT_IMAGE_TAG_VIRTUAL;
        pullImageFromRemote = StringUtils.appendIfMissing(containerRegistry, "/") + remoteRepo + "/" + "hello-world:latest";
        pullImageFromVirtual = StringUtils.appendIfMissing(virtualDomainName, "/") + "hello-world:latest";
        envVars = new HashMap<String, String>(System.getenv()) {{
            putIfAbsent(BITESTS_ENV_VAR_PREFIX + "USERNAME", getUsername());
            putIfAbsent(BITESTS_ENV_VAR_PREFIX + "ADMIN_TOKEN", getAdminToken());
            putIfAbsent("BITESTS_ARTIFACTORY_DOCKER_VIRTUAL_DOMAIN", virtualDomainName);
        }};
    }

    @Test
    public void dockerPushToLocalTest() {
        DockerJavaWrapper.buildImage(imageTagLocal, host, Collections.emptyMap(), PROJECT_PATH);
        DockerPush dockerPush = new DockerPush(artifactoryManagerBuilder, imageTagLocal, host, artifactProperties, localRepo1, getUsername(), getAdminToken(), getLog(), Collections.emptyMap());
        pushAndValidateImage(dockerPush, localRepo1, imageTagLocal, SHORT_IMAGE_TAG_LOCAL);
    }

    @Test
    public void dockerPushToVirtualTest() {
        DockerJavaWrapper.buildImage(imageTagVirtual, host, Collections.emptyMap(), PROJECT_PATH);
        DockerPush dockerPush = new DockerPush(artifactoryManagerBuilder, imageTagVirtual, host, artifactProperties, virtualRepo, getUsername(), getAdminToken(), getLog(), Collections.emptyMap());
        pushAndValidateImage(dockerPush, virtualRepo, imageTagVirtual, SHORT_IMAGE_TAG_VIRTUAL);
    }

    @Test
    public void dockerPullFromRemoteTest() {
        DockerPull dockerPull = new DockerPull(artifactoryManagerBuilder, pullImageFromRemote, host, remoteRepo, getUsername(), getAdminToken(), getLog(), Collections.emptyMap());
        validatePulledDockerImage(dockerPull.execute(), pullImageFromRemote);
    }

    @Test
    public void dockerPullFromVirtualTest() {
        DockerPull dockerPull = new DockerPull(artifactoryManagerBuilder, pullImageFromVirtual, host, virtualRepo, getUsername(), getAdminToken(), getLog(), Collections.emptyMap());
        validatePulledDockerImage(dockerPull.execute(), pullImageFromVirtual);
    }

    @Test
    public void buildDockerCreateKanikoTest() throws IOException, InterruptedException {
        Path workingDirectory = Files.createTempDirectory("build-docker-create-kaniko-test",
                PosixFilePermissions.asFileAttribute(Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE)));
        try {
            FileUtils.copyDirectory(new File(PROJECT_PATH), workingDirectory.toFile());
            Path kanikoConfig = createKanikoConfig(workingDirectory, "artifactory:8082");
            String kanikoFile = execKaniko(workingDirectory, pullImageFromVirtual.replace("127.0.0.1:8081", "artifactory:8082"), kanikoConfig);

            BuildDockerCreator buildDockerCreator = new BuildDockerCreator(artifactoryManagerBuilder, kanikoFile, BuildDockerCreator.ImageFileType.KANIKO, artifactProperties, virtualRepo, getLog());
            BuildInfo buildInfo = buildDockerCreator.execute();
            assertEquals(buildInfo.getModules().size(), 1);
            Module module = getAndValidateModule(buildInfo, "hello-world:latest", virtualRepo);
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
            BuildInfo buildInfo = new BuildDockerCreator(artifactoryManagerBuilder, getJibImageJsonPath(wd),
                    imageFileType, artifactProperties, virtualRepo, getLog()).execute();

            // Check modules
            assertEquals(buildInfo.getModules().size(), 3);
            Module module = getAndValidateModule(buildInfo, "multi1", virtualRepo);
            assertFalse(module.getArtifacts().isEmpty());
            module = getAndValidateModule(buildInfo, "multi2", virtualRepo);
            assertFalse(module.getArtifacts().isEmpty());
            module = getAndValidateModule(buildInfo, "multi3", virtualRepo);
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
                .put("password", getAdminToken());
        ObjectNode registryNode = mapper.createObjectNode().set(registry, credentials);
        String kanikoConfig = mapper.createObjectNode().set("auths", registryNode).toPrettyString();
        Files.write(kanikoConfigPath, kanikoConfig.getBytes(StandardCharsets.UTF_8));
        return kanikoConfigPath;
    }

    private String execKaniko(Path workingDirectory, String registry, Path kanikoConfigPath) throws IOException, InterruptedException {
        CommandExecutor commandExecutor = new CommandExecutor("docker", null);
        List<String> args = Lists.newArrayList("run", "--network=test-network", "--rm", "-v",
                workingDirectory.toAbsolutePath() + ":/workspace", "-v",
                kanikoConfigPath + ":/kaniko/.docker/config.json:ro", "gcr.io/kaniko-project/executor:latest",
                "--dockerfile=Dockerfile", "--destination=" + registry, "--insecure", "--skip-tls-verify",
                "--image-name-tag-with-digest-file=image-file");
        CommandResults results = commandExecutor.exeCommand(workingDirectory.toFile(), args, null, getLog());
        assertTrue(results.isOk(), results.getRes() + ":" + results.getErr());
        return workingDirectory.resolve("image-file").toAbsolutePath().toString();
    }

    private void execJib(Path workingDirectory) throws IOException, InterruptedException {
        CommandExecutor commandExecutor = new CommandExecutor("mvn", envVars);
        List<String> args = Lists.newArrayList("compile", "-B", "jib:build", "-DsendCredentialsOverHttp");
        CommandResults results = commandExecutor.exeCommand(workingDirectory.toFile(), args, null, getLog());
        assertTrue(results.isOk(), results.getRes() + ": " + results.getErr());
    }

    private String getJibImageJsonPath(Path workingDirectory) {
        return workingDirectory.resolve("*").resolve("target").resolve("jib-image.json").toAbsolutePath().toString();
    }

    private void pushAndValidateImage(DockerPush dockerPush, String repo, String imageTag, String shortImageTag) {
        BuildInfo buildInfo = dockerPush.execute();
        Module module = getAndValidateModule(buildInfo, SHORT_IMAGE_NAME + ":" + shortImageTag, repo);
        List<Artifact> artifacts = module.getArtifacts();
        validateImageArtifacts(artifacts, imageTag);
        assertEquals(7, artifacts.size());
        module.getArtifacts().forEach(artifact -> assertEquals(artifact.getRemotePath(), SHORT_IMAGE_NAME + "/" + shortImageTag));
        assertEquals(5, module.getDependencies().size());
    }

    private void validatePulledDockerImage(BuildInfo buildInfo, String image) {
        assertEquals(buildInfo.getModules().size(), 1);
        Module module = buildInfo.getModules().get(0);
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

    private Module getAndValidateModule(BuildInfo buildInfo, String id, String repo) {
        Module module = buildInfo.getModules().stream()
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
