package org.jfrog.gradle.plugin.artifactory;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.VersionNumber;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.testng.Assert.assertEquals;

/**
 * @author yahavi
 */
@Test
public class GradlePluginTest extends IntegrationTestsBase {

    private static final VersionNumber GRADLE_6 = VersionNumber.parse("6.0");
    private static final String GRADLE_LOCAL_REPO = "build-info-tests-gradle-local";
    private static final String GRADLE_REMOTE_REPO = "build-info-tests-gradle-remote";
    private static final String GRADLE_VIRTUAL_REPO = "build-info-tests-gradle-virtual";

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "gradle", "plugin", "artifactory"));
    private static final File GRADLE_EXAMPLE_DIR = PROJECTS_ROOT.resolve("gradle-example").toFile();

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private DependenciesDownloaderHelper downloaderHelper;

    public GradlePluginTest() {
        localRepo = GRADLE_LOCAL_REPO;
        remoteRepo = GRADLE_REMOTE_REPO;
        virtualRepo = GRADLE_VIRTUAL_REPO;
    }

    @DataProvider
    private Object[][] gradleVersions() {
        return new Object[][]{{"4.10.3"}};
    }

    @Test(dataProvider = "gradleVersions")
    public void configurationsTest(String gradleVersion) {
        BuildResult buildResult = runGradle(gradleVersion, GRADLE_EXAMPLE_DIR);
        assertEquals(buildResult.task(":services:webservice:artifactoryPublish").getOutcome(), SUCCESS);
    }

    BuildResult runGradle(String gradleVersion, File projectDir) {
        List<String> arguments = Arrays.asList("--stacktrace", "clean", "artifactoryPublish");
        return GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(projectDir)
                .withPluginClasspath(Arrays.asList(Paths.get(".").toAbsolutePath().getParent().resolve("src/main/groovy/org/jfrog/gradle/plugin/artifactory").toFile()))
                .withArguments(arguments)
                .build();
    }
}
