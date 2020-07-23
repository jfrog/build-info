package org.jfrog.build.extractor.nuget.extractor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class NugetExtractorTest extends IntegrationTestsBase {

    private static final String NUGET_LOCAL_REPO = "build-info-tests-npm-local";
    private static final String NUGET_REMOTE_REPO = "build-info-tests-npm-remote";
    private static final String NUGET_VIRTUAL_REPO = "build-info-tests-npm-virtual";

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;

    public NugetExtractorTest() {
        localRepo = NUGET_LOCAL_REPO;
        remoteRepo = NUGET_REMOTE_REPO;
        virtualRepo = NUGET_VIRTUAL_REPO;
    }

    private enum Project {
        // Test projects
        PACKAGESCONFIG("packagesconfig", "NugetExtractorTest PackagesConfig", "package-config", 6),
        REFERENCE("reference", "NugetExtractorTest Reference", "reference", 6);


        private File projectOrigin;
        private String targetDir;
        private String name;
        private Integer[] dependenciesCount;

        Project(String sourceDir, String targetDir, String name, Integer... dependenciesCounts) {
            this.projectOrigin = PROJECTS_ROOT.resolve(sourceDir).toFile();
            this.targetDir = targetDir;
            this.name = name;
            this.dependenciesCount = dependenciesCounts;
        }

    }

    @BeforeClass
    private void setUp() throws IOException {
        dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        buildInfoClientBuilder = new ArtifactoryBuildInfoClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
    }

    @DataProvider
    private Object[][] nugetRunProvider() {
        return new Object[][]{
                {Project.PACKAGESCONFIG, "restore", null},
                {Project.REFERENCE, "restore", null},
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "nugetRunProvider")
    private void nugetRunTest(Project project, String args, String Name) {
        Path projectDir = null;
        try {
            // Run nuget restore install
            projectDir = createProjectDir(project);
            NugetRun nugetRun = new NugetRun(dependenciesClientBuilder, remoteRepo, args, log, projectDir, null, null, getUsername(), getPassword());
            Build build = nugetRun.execute();
            assertEquals(build.getModules().size(), project.dependenciesCount.length);
            Module module = build.getModules().get(0);
            // Check correctness of the module and dependencies
            assertEquals(module.getDependencies().size(), project.dependenciesCount);
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
