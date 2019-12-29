package org.jfrog.build.extractor.go.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

@Test
public class GoExtractorTest extends IntegrationTestsBase {

    private static final String GO_LOCAL_REPO = "build-info-tests-go-local";
    private static final String GO_REMOTE_REPO = "build-info-tests-go-remote";
    private static final String GO_VIRTUAL_REPO = "build-info-tests-go-virtual";
    private static final String[] GO_PACKAGE_EXTENSIONS = {".zip", ".mod", ".info"};
    private static final String GO_BUILD_CMD = "build";

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    private ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private Map<String, String> env;

    public GoExtractorTest() {
        localRepo = GO_LOCAL_REPO;
        remoteRepo = GO_REMOTE_REPO;
        virtualRepo = GO_VIRTUAL_REPO;
        env = new TreeMap<>();
        // Since we are handling dummy projects, we want to avoid package validation against Go's checksum DB.
        env.put("GONOSUMDB", "github.com/jfrog");
    }

    private enum Project {
        // Dependencies
        SAMPLER("sampler", "", "rsc.io/sampler", "1.3.0"),
        TEXT("text", "", "golang.org/x/text", "0.0.0-20170915032832-14c0d48ead0c"),
        QUOTE("quote", "", "rsc.io/quote", "1.5.2"),

        // Test projects
        PROJECT_1("project1", "jfrog-dependency", "github.com/jfrog/dependency", "1.0.0", SAMPLER.getDependencyId(), TEXT.getDependencyId(), QUOTE.getDependencyId()),
        PROJECT_2("project2", "jfrog-project", "github.com/jfrog/project", "1.0.0", SAMPLER.getDependencyId(), TEXT.getDependencyId(), QUOTE.getDependencyId(), PROJECT_1.getDependencyId());

        private File projectOrigin;
        private String targetDir;
        private String name;
        private String version;
        private Set<String> dependencies;

        Project(String projectDir, String targetDir, String name, String version, String... dependencies) {
            this.projectOrigin = PROJECTS_ROOT.resolve(projectDir).toFile();
            this.targetDir = targetDir;
            this.name = name;
            this.version = version;
            this.dependencies = Sets.newHashSet(dependencies);
        }

        private String getDependencyId() {
            return String.format("%s:v%s", name, version);
        }

        private String getModuleId() {
            return name;
        }

        private String getVersion() {
            return version;
        }

        private String getTargetPath(String extention) {
            return String.format("%s/@v/v%s.%s", name, version, extention);
        }

        private Set<String> getArtifactSet() {
            Set<String> artifacts = new HashSet<>();
            for (String ext : GO_PACKAGE_EXTENSIONS) {
                artifacts.add(String.format("%s:v%s%s", name, version, ext));
            }
            return artifacts;
        }
    }

    @BeforeClass
    private void setUp() throws IOException {
        buildInfoClientBuilder = new ArtifactoryBuildInfoClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        deployTestDependencies(Project.QUOTE, Project.SAMPLER, Project.TEXT);
    }

    private void deployTestDependencies(Project... projects) throws IOException {
        for (Project project : projects) {
            for (String ext : GO_PACKAGE_EXTENSIONS) {
                File pkgFile = project.projectOrigin.toPath().resolve("v" + project.version + ext).toFile();
                if (!pkgFile.exists())
                    continue;
                DeployDetails deployDetails = new DeployDetails.Builder()
                        .file(pkgFile)
                        .targetRepository(localRepo)
                        .artifactPath(project.getTargetPath(ext))
                        .build();
                buildInfoClient.deployArtifact(deployDetails);
            }
        }
    }

    @BeforeMethod
    private void cleanGoModCache() throws IOException, InterruptedException {
        CommandExecutor goCommandExecutor = new CommandExecutor("go", null);
        List<String> goCleanArgs = new ArrayList<>();
        goCleanArgs.add("clean");
        goCleanArgs.add("-modcache");
        goCommandExecutor.exeCommand(PROJECTS_ROOT.toFile(), goCleanArgs, log);
    }


    // We want to build project1 locally (with default Go proxy), and then with Artifactory for dependencies resolution.
    @DataProvider
    private Object[][] goRunProvider() {
        return new Object[][]{
                {Project.PROJECT_1, GO_BUILD_CMD, null, StringUtils.EMPTY},
                {Project.PROJECT_1, GO_BUILD_CMD, buildInfoClientBuilder, localRepo},
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "goRunProvider")
    private void goRunTest(Project project, String args, ArtifactoryBuildInfoClientBuilder clientBuilder, String repo) {
        // The go tests are currently disabled on Windows.
        // This is because “go build” fails due to “Access is denied” when invoked from the tests.
        if (SystemUtils.IS_OS_WINDOWS)
            return;
        Path projectDir = null;
        try {
            // Run Go build
            projectDir = createProjectDir(project);
            GoRun goRun = new GoRun(args, projectDir, clientBuilder, repo, getUsername(), getPassword(), getLog(), env);
            Build build = goRun.execute();
            // Check successful execution and correctness of the module and dependencies
            assertNotNull(build);
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);
            assertEquals(module.getId(), project.getModuleId());
            Set<String> moduleDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
            assertEquals(moduleDependencies, project.dependencies);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    /*
     * 1. Build project1 locally (with default Go proxy)
     * 2. Publish project1 package to Artifactory. (Project1 is a dependency for project2)
     * 3. Build project2 with Artifactory for resolution.
     */
    @Test
    private void goRunPublishTest() {
        // The go tests are currently disabled on Windows.
        // This is because “go build” fails due to “Access is denied” when invoked from the tests.
        if (SystemUtils.IS_OS_WINDOWS)
            return;
        Path projectDir = null;
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
        try {
            // Run Go build on project1 locally
            Project project = Project.PROJECT_1;
            projectDir = createProjectDir(project);
            GoRun goRun = new GoRun(GO_BUILD_CMD, projectDir, null, StringUtils.EMPTY, getUsername(), getPassword(), getLog(), env);
            Build project1Build = goRun.execute();
            // Check successful execution
            assertNotNull(project1Build);
            // Publish project1 to Artifactory
            GoPublish goPublish = new GoPublish(buildInfoClientBuilder, properties, localRepo, projectDir, project.getVersion(), getLog());
            Build publishBuild = goPublish.execute();
            // Check successful execution
            assertNotNull(publishBuild);
            project1Build.append(publishBuild);
            // Check correctness of the module, dependencies and artifacts
            assertEquals(project1Build.getModules().size(), 1);
            Module module = project1Build.getModules().get(0);
            assertEquals(module.getId(), project.getModuleId());
            Set<String> moduleDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
            assertEquals(moduleDependencies, project.dependencies);
            Set<String> moduleArtifacts = module.getArtifacts().stream().map(Artifact::getName).collect(Collectors.toSet());
            assertEquals(moduleArtifacts, project.getArtifactSet());

            // Run Go build on project2 using Artifctory for resolution
            project = Project.PROJECT_2;
            projectDir = createProjectDir(project);
            goRun = new GoRun(GO_BUILD_CMD, projectDir, buildInfoClientBuilder, virtualRepo, getUsername(), getPassword(), getLog(), env);
            Build project2Build = goRun.execute();
            // Check successful execution and correctness of the module, dependencies and artifacts
            assertNotNull(project2Build);
            assertEquals(project2Build.getModules().size(), 1);
            module = project2Build.getModules().get(0);
            assertEquals(module.getId(), project.getModuleId());
            moduleDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
            assertEquals(moduleDependencies, project.dependencies);
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
