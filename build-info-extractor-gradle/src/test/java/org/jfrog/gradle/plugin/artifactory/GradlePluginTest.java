package org.jfrog.gradle.plugin.artifactory;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.util.VersionNumber;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.gradle.plugin.artifactory.Consts.*;
import static org.jfrog.gradle.plugin.artifactory.Utils.*;

/**
 * @author yahavi
 */
@Test
public class GradlePluginTest extends IntegrationTestsBase {

    final private Map<String, String> envVars;

    public GradlePluginTest() {
        localRepo1 = GRADLE_LOCAL_REPO;
        remoteRepo = GRADLE_REMOTE_REPO;
        virtualRepo = GRADLE_VIRTUAL_REPO;

        // Set environment variables for variable replacement in build.gradle files
        envVars = new HashMap<String, String>(System.getenv()) {{
            putIfAbsent(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "URL", getUrl());
            putIfAbsent(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "USERNAME", getUsername());
            putIfAbsent(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "PASSWORD", getPassword());
            putIfAbsent(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "LOCAL_REPO", localRepo1);
            putIfAbsent(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "VIRTUAL_REPO", virtualRepo);
        }};
    }

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        deleteTestDir();
        deleteContentFromRepo(localRepo1);
    }

    @DataProvider
    private Object[][] gradleVersions() {
        return new String[][]{{"4.10.3"}, {"5.6.4"}, {"6.3"}};
    }

    @Test(dataProvider = "gradleVersions")
    public void configurationsTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE);
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, envVars, false);
        // Check results
        checkBuildResults(dependenciesClient, buildInfoClient, buildResult, false, getUrl(), localRepo1);
    }

    @Test(dataProvider = "gradleVersions")
    public void publicationsTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_PUBLISH);
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, envVars, false);
        // Check results
        checkBuildResults(dependenciesClient, buildInfoClient, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, getUrl(), localRepo1);
    }

    @Test(dataProvider = "gradleVersions")
    public void ciServerTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getUrl(), getUsername(), getPassword(), localRepo1, virtualRepo, "", true);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBuildResults(dependenciesClient, buildInfoClient, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, getUrl(), localRepo1);
    }

    @Test(dataProvider = "gradleVersions")
    public void ciServerPublicationsTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getUrl(), getUsername(), getPassword(), localRepo1, virtualRepo, "mavenJava,customIvyPublication", true);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBuildResults(dependenciesClient, buildInfoClient, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, getUrl(), localRepo1);
    }

    @Test(dataProvider = "gradleVersions")
    public void requestedByTest(String gradleVersion) throws IOException {

        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getUrl(), getUsername(), getPassword(), localRepo1, virtualRepo, "mavenJava,customIvyPublication", false);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        Path buildInfo = TEST_DIR.toPath().resolve(Paths.get("build", "build-info.json"));
        checkRequestedBy(buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, buildInfo.toFile());
    }
}
