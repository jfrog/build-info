package org.jfrog.build.extractor.npm.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * @author Yahav Itzhak
 */
@Test
public class NpmExtractorTest extends IntegrationTestsBase {

    private static final String NPM_LOCAL_REPO = "build-info-tests-npm-local";
    private static final String NPM_REMOTE_REPO = "build-info-tests-npm-remote";
    private static final String NPM_VIRTUAL_REPO = "build-info-tests-npm-virtual";

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private DependenciesDownloaderHelper downloaderHelper;
    private NpmDriver driver = new NpmDriver("", null);

    public NpmExtractorTest() {
        localRepo = NPM_LOCAL_REPO;
        remoteRepo = NPM_REMOTE_REPO;
        virtualRepo = NPM_VIRTUAL_REPO;
    }

    private enum Project {
        // Dependencies
        ASGARD("asgard", "jfrog-asgard", "jfrog-asgard", "2.0.0"),
        MIDGARD("midgard", "jfrog-midgard", "jfrog-midgard", "1.0.0"),
        ALFHEIM("alfheim", "jfrog-alfheim", "jfrog-alfheim", "3.5.2"),
        SVARTALFHEIM("svartalfheim", "jfrog-svartalfheim", "jfrog-svartalfheim", "0.5.0"),

        // Test projects
        A("a", "NpmExtractorTest Project A", "package-name1", "0.0.1", ASGARD.getPackedFileName(), SVARTALFHEIM.getPackedFileName()),
        B("b", "NpmExtractorTest-Project-B", "package-name2", "0.0.2", ASGARD.getPackedFileName(), MIDGARD.getPackedFileName(), ALFHEIM.getPackedFileName()),
        C("c", "NpmExtractorTestProjectC", "package-name3", "0.0.3", ASGARD.getPackedFileName(), MIDGARD.getPackedFileName(), ALFHEIM.getPackedFileName(), SVARTALFHEIM.getPackedFileName());

        private File projectOrigin;
        private String targetDir;
        private String name;
        private String version;
        private Set<String> dependencies;

        Project(String sourceDir, String targetDir, String name, String version, String... dependencies) {
            this.projectOrigin = PROJECTS_ROOT.resolve(sourceDir).toFile();
            this.targetDir = targetDir;
            this.name = name;
            this.version = version;
            this.dependencies = Sets.newHashSet(dependencies);
        }

        private String getModuleId() {
            return String.format("%s:%s", name, version);
        }

        private String getPackedFileName() {
            return String.format("%s-%s.tgz", name, version);
        }

        private String getTargetPath() {
            return String.format("%s/-/%s", name, getPackedFileName());
        }
    }

    @BeforeClass
    private void setUp() throws IOException {
        dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        buildInfoClientBuilder = new ArtifactoryBuildInfoClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        downloaderHelper = new DependenciesDownloaderHelper(dependenciesClient, ".", log);
        deployTestDependencies(Project.ASGARD, Project.MIDGARD, Project.ALFHEIM, Project.SVARTALFHEIM);
    }

    private void deployTestDependencies(Project... projects) throws IOException {
        for (Project project : projects) {
            driver.pack(project.projectOrigin, Collections.emptyList());
            DeployDetails deployDetails = new DeployDetails.Builder()
                    .file(project.projectOrigin.toPath().resolve(project.getPackedFileName()).toFile())
                    .targetRepository(localRepo)
                    .artifactPath(project.getTargetPath())
                    .build();
            buildInfoClient.deployArtifact(deployDetails);
        }
    }

    @AfterClass
    private void tearDown() throws IOException {
        deletePackedFiles(Project.ASGARD, Project.MIDGARD, Project.ALFHEIM, Project.SVARTALFHEIM);
    }

    private void deletePackedFiles(Project... projects) throws IOException {
        for (Project project : projects) {
            Files.deleteIfExists(project.projectOrigin.toPath().resolve(project.getPackedFileName()));
        }
    }

    @DataProvider
    private Object[][] npmInstallProvider() {
        return new Object[][]{
                {Project.A, Project.A.dependencies, "", true},
                {Project.A, Collections.emptySet(), "--only=dev", false},
                {Project.A, Project.A.dependencies, "--only=prod", true},
                {Project.B, Project.B.dependencies, "", false},
                {Project.B, Project.B.dependencies, "--only=dev", true},
                {Project.B, Collections.emptySet(), "--production", false},
                {Project.C, Project.C.dependencies, "", true},
                {Project.C, Project.B.dependencies, "--only=development", false},
                {Project.C, Project.A.dependencies, "--only=production", true},
                {Project.C, Project.C.dependencies, "--verbose", true}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmInstallProvider")
    private void npmInstallTest(Project project, Set<String> expectedDependencies, String args, boolean packageJsonPath) {
        Path projectDir = null;
        try {
            // Run npm install
            projectDir = createProjectDir(project);
            Path path = packageJsonPath ? projectDir.resolve("package.json") : projectDir;
            NpmInstall npmInstall = new NpmInstall(dependenciesClientBuilder, virtualRepo, args, null, log, path, null);
            Build build = npmInstall.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);
            // Check correctness of the module and dependencies
            assertEquals(module.getId(), project.getModuleId());
            Set<String> moduleDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
            assertEquals(moduleDependencies, expectedDependencies);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    @DataProvider
    private Object[][] npmPublishProvider() {
        return new Object[][]{
                {Project.A, ArrayListMultimap.create(), Project.A.getTargetPath(), ""},
                {Project.A, ArrayListMultimap.create(ImmutableMultimap.of("a", "b")), Project.A.getTargetPath(), ""},
                {Project.B, ArrayListMultimap.create(), Project.B.getTargetPath(), Project.B.getPackedFileName()},
                {Project.B, ArrayListMultimap.create(ImmutableMultimap.of("a", "b", "c", "d")), Project.B.getTargetPath(), Project.B.getPackedFileName()},
                {Project.C, ArrayListMultimap.create(), Project.C.getTargetPath(), ""},
                {Project.C, ArrayListMultimap.create(ImmutableMultimap.of("a", "b", "a", "d")), Project.C.getTargetPath(), ""}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmPublishProvider")
    private void npmPublishTest(Project project, ArrayListMultimap<String, String> props, String targetPath, String packageName) {
        Path projectDir = null;
        try {
            // Run npm publish
            projectDir = createProjectDir(project);
            Path path = StringUtils.isNotBlank(packageName) ? projectDir.resolve(packageName) : projectDir;
            NpmPublish npmPublish = new NpmPublish(buildInfoClientBuilder, props, null, path, virtualRepo, log, null);
            Build build = npmPublish.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);

            // Check correctness of the module and the artifact
            assertEquals(module.getId(), project.getModuleId());
            assertEquals(module.getArtifacts().size(), 1);
            assertEquals(module.getArtifacts().get(0).getName(), project.getModuleId());

            // Download the artifact and check for its properties
            StringJoiner propertiesBuilder = new StringJoiner(";");
            props.entries().forEach(property -> propertiesBuilder.add(property.getKey() + "=" + property.getValue()));
            FileSpec fileSpec = new FileSpec();
            fileSpec.setProps(propertiesBuilder.toString());
            fileSpec.setPattern(localRepo + "/" + targetPath);
            fileSpec.setTarget(projectDir.toString());

            Spec spec = new Spec();
            spec.setFiles(new FileSpec[]{fileSpec});
            assertEquals(downloaderHelper.downloadDependencies(spec).size(), 1);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    private Path createProjectDir(Project project) throws IOException {
        File projectDir = Files.createTempDirectory(project.targetDir).toFile();
        FileUtils.copyDirectory(project.projectOrigin, projectDir);
        return projectDir.toPath();
    }
}
