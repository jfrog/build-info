package org.jfrog.gradle.plugin.artifactory;

import org.apache.commons.lang3.tuple.Pair;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.util.VersionNumber;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.client.Version;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.gradle.plugin.artifactory.Consts.*;
import static org.jfrog.gradle.plugin.artifactory.Utils.*;

/**
 * @author yahavi
 */
@Test
public class GradlePluginTest extends IntegrationTestsBase {

    private Map<String, String> envVars;

    public GradlePluginTest() {
        localRepo1 = getKeyWithTimestamp(GRADLE_LOCAL_REPO);
        remoteRepo = getKeyWithTimestamp(GRADLE_REMOTE_REPO);
        virtualRepo = getKeyWithTimestamp(GRADLE_VIRTUAL_REPO);
    }

    @BeforeClass
    public void init() throws IOException {
        super.init();
        // Set environment variables for variable replacement in build.gradle files
        envVars = new HashMap<String, String>(System.getenv()) {{
            putIfAbsent(BITESTS_ENV_VAR_PREFIX + "URL", getPlatformUrl());
            putIfAbsent(BITESTS_ENV_VAR_PREFIX + "USERNAME", getUsername());
            putIfAbsent(BITESTS_ENV_VAR_PREFIX + "ADMIN_TOKEN", getAdminToken());
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
        return new String[][]{{"4.10.3"}, {"5.6.4"}, {"6.9.3"}, {"7.5.1"}};
    }

    @Test(dataProvider = "gradleVersions")
    public void configurationsTest(String gradleVersion) throws IOException {
        if (new Version(gradleVersion).isAtLeast(MIN_VERSION_WITHOUT_CONFIGURATIONS)) {
            throw new SkipException("Skipping test on Gradle 7");
        }
        // Create test environment
        createTestDir(GRADLE_EXAMPLE);
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, envVars, false);
        // Check results
        checkBuildResults(artifactoryManager, buildResult, false, localRepo1);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void publicationsTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_PUBLISH);
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, envVars, false);
        // Check results
        checkBuildResults(artifactoryManager, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, localRepo1);
        checkArtifactsProps(artifactoryManager);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void publicationsTestKotlinDsl(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_KTS_EXAMPLE_PUBLISH);
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, envVars, false);
        // Check results
        checkBuildResults(artifactoryManager, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, localRepo1);
        checkArtifactsProps(artifactoryManager);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void ciServerTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "", true, true, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBuildResults(artifactoryManager, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, localRepo1);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    /**
     * Gradle extractor may be run by CI servers such as Jenkins. Before the CI server runs the Gradle extractor in order to build the Gradle project,
     * it generated build-info properties file that contains Gradle extractor's configurations. Those generated properties could be deprecated but the Gradle extractor could be the latest version (according to the build.gradle).
     * This test checks that the deprecated build-info properties are being handled correctly
     */
    @Test(dataProvider = "gradleVersions")
    public void deprecatedCiServerTest(String gradleVersion) throws IOException {
        if (new Version(gradleVersion).isAtLeast(MIN_VERSION_WITHOUT_CONFIGURATIONS)) {
            throw new SkipException("Skipping test on Gradle 7");
        }
        // Create test environment
        createTestDir(DEPRECATED_GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "", true, true, DEPRECATED_BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, DEPRECATED_BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        // Assert all tasks ended with success outcome
        assertProjectsSuccess(buildResult);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void ciServerPublicationsTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "mavenJava,customIvyPublication", true, true, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBuildResults(artifactoryManager, buildResult, VersionNumber.parse(gradleVersion).getMajor() >= 6, localRepo1);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void requestedByTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "mavenJava,customIvyPublication", false, true, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        int expectedArtifactsPerModule = VersionNumber.parse(gradleVersion).getMajor() >= 6 ? 5 : 4;
        checkLocalBuild(buildResult, BUILD_INFO_JSON.toFile(), 3, expectedArtifactsPerModule);
        // Cleanup
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        cleanTestBuilds(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    @Test(dataProvider = "gradleVersions")
    public void ciServerResolverOnlyTest(String gradleVersion) throws IOException {
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CI_SERVER);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "", false, false, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkLocalBuild(buildResult, BUILD_INFO_JSON.toFile(), 2, 0);
    }

    /**
     * A test for publishing bom files (pom.xml with dependencyManagement) using the default mavenJavaPlatform publication.
     */
    @Test(dataProvider = "gradleVersions")
    public void publishDefaultBomTest(String gradleVersion) throws IOException {
        if (!new Version(gradleVersion).isAtLeast(MIN_VERSION_FOR_BOM)) {
            throw new SkipException("Skipping test on Gradle 7");
        }
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_DEFAULT_BOM);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "", true, true, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBomBuild(buildResult, BUILD_INFO_JSON.toFile(), VersionNumber.parse(gradleVersion).getMajor() >= 6 ? 2 : 1);
    }

    /**
     * A test for publishing bom files (pom.xml with dependencyManagement) using the customMavenJavaPlatform publication.
     */
    @Test(dataProvider = "gradleVersions")
    public void publishCustomBomTest(String gradleVersion) throws IOException {
        if (!new Version(gradleVersion).isAtLeast(MIN_VERSION_FOR_BOM)) {
            throw new SkipException("Skipping test on Gradle 7");
        }
        // Create test environment
        createTestDir(GRADLE_EXAMPLE_CUSTOM_BOM);
        generateBuildInfoProperties(getArtifactoryUrl(), getUsername(), getAdminToken(), localRepo1, virtualRepo, "customMavenJavaPlatform", true, true, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        Map<String, String> extendedEnv = new HashMap<String, String>(envVars) {{
            put(BuildInfoConfigProperties.PROP_PROPS_FILE, BUILD_INFO_PROPERTIES_TARGET.toString());
        }};
        // Run Gradle
        BuildResult buildResult = runGradle(gradleVersion, extendedEnv, true);
        // Check results
        checkBomBuild(buildResult, BUILD_INFO_JSON.toFile(), VersionNumber.parse(gradleVersion).getMajor() >= 6 ? 2 : 1);
    }
}
