package org.jfrog.gradle.plugin.artifactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.lang3.tuple.Pair;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.jsonStringToBuildInfo;
import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.PublishBuildInfo.BUILD_BROWSE_URL;
import static org.jfrog.gradle.plugin.artifactory.Consts.ARTIFACTS_GROUP_ID;
import static org.jfrog.gradle.plugin.artifactory.Consts.BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER;
import static org.jfrog.gradle.plugin.artifactory.Consts.BUILD_INFO_PROPERTIES_SOURCE_RESOLVER;
import static org.jfrog.gradle.plugin.artifactory.Consts.BUILD_INFO_PROPERTIES_TARGET;
import static org.jfrog.gradle.plugin.artifactory.Consts.EXPECTED_ARTIFACTS;
import static org.jfrog.gradle.plugin.artifactory.Consts.EXPECTED_MODULE_ARTIFACTS;
import static org.jfrog.gradle.plugin.artifactory.Consts.INIT_SCRIPT;
import static org.jfrog.gradle.plugin.artifactory.Consts.LIBS_DIR;
import static org.jfrog.gradle.plugin.artifactory.Consts.TEST_DIR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * @author yahavi
 */
public class Utils {

    /**
     * Copy the test project from sourceDir to TEST_DIR.
     *
     * @param sourceDir - The Gradle project
     * @throws IOException - In case of any IO error
     */
    static void createTestDir(Path sourceDir) throws IOException {
        FileUtils.copyDirectory(sourceDir.toFile(), TEST_DIR);
    }

    /**
     * Delete the tests directories
     *
     * @throws IOException - In case of any IO error
     */
    static void deleteTestDir() throws IOException {
        FileUtils.deleteDirectory(TEST_DIR);
    }

    /**
     * Run Gradle process with the GradleRunner.
     *
     * @param gradleVersion   - The Gradle version to use
     * @param envVars         - The environment variables
     * @param applyInitScript - True iff the gradle.init script should be applied
     * @return the build results
     * @throws IOException - In case of any IO error
     */
    static BuildResult runGradle(String gradleVersion, Map<String, String> envVars, boolean applyInitScript) throws IOException {
        List<String> arguments = new ArrayList<>(Arrays.asList("clean", "artifactoryPublish", "--stacktrace"));
        if (applyInitScript) {
            generateInitScript();
            arguments.add("--init-script=gradle.init");
        }
        //noinspection UnstableApiUsage
        return GradleRunner.create()
                .withGradleVersion(gradleVersion)
                .withProjectDir(TEST_DIR)
                .withPluginClasspath()
                .withArguments(arguments)
                .withEnvironment(envVars)
                .build();
    }

    /**
     * Generate buildinfo.properties file with all details from user.
     *
     * @param contextUrl       - Artifactory URL
     * @param username         - Artifactory username
     * @param password         - Artifactory password
     * @param localRepo        - Gradle local repository
     * @param virtualRepo      - Gradle remote repository
     * @param publishBuildInfo - Publish build info
     * @param setDeployer      - Set deployer details in file
     * @throws IOException - In case of any IO error
     */
    static void generateBuildInfoProperties(String contextUrl, String username, String password, String localRepo, String virtualRepo, String publications, boolean publishBuildInfo, boolean setDeployer) throws IOException {
        String content = generateBuildInfoPropertiesForServer(contextUrl, username, password, localRepo, virtualRepo, publications, publishBuildInfo, BUILD_INFO_PROPERTIES_SOURCE_RESOLVER);
        if (setDeployer) {
            content += "\n";
            content += generateBuildInfoPropertiesForServer(contextUrl, username, password, localRepo, virtualRepo, publications, publishBuildInfo, BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER);
        }
        Files.write(BUILD_INFO_PROPERTIES_TARGET, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate buildinfo.properties section from source template.
     *
     * @param contextUrl       - Artifactory URL
     * @param username         - Artifactory username
     * @param password         - Artifactory password
     * @param localRepo        - Gradle local repository
     * @param virtualRepo      - Gradle remote repository
     * @param publishBuildInfo - Publish build info
     * @param source           - Path to server specific buildinfo.properties template.
     * @throws IOException - In case of any IO error
     */
    private static String generateBuildInfoPropertiesForServer(String contextUrl, String username, String password, String localRepo, String virtualRepo, String publications, boolean publishBuildInfo, Path source) throws IOException {
        String content = new String(Files.readAllBytes(source), StandardCharsets.UTF_8);
        Map<String, String> valuesMap = new HashMap<String, String>() {{
            put("publications", publications);
            put("contextUrl", contextUrl);
            put("username", username);
            put("password", password);
            put("localRepo", localRepo);
            put("virtualRepo", virtualRepo);
            put("buildInfo", String.valueOf(publishBuildInfo));
        }};
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        return sub.replace(content);
    }

    /**
     * 1. Check the build status of all tasks.
     * 2. Make sure all artifacts deployed.
     *
     * @param artifactoryManager    - ArtifactoryManager client
     * @param buildResult           - The build results
     * @param expectModuleArtifacts - Should we expect *.module files
     * @param localRepo             - Artifactory local localRepo
     * @throws IOException - In case of any IO error
     */
    static void checkBuildResults(ArtifactoryManager artifactoryManager, BuildResult buildResult, boolean expectModuleArtifacts, String localRepo) throws IOException {
        // Assert all tasks ended with success outcome
        assertProjectsSuccess(buildResult);

        // Check that all expected artifacts uploaded to Artifactory
        String[] expectedArtifacts = expectModuleArtifacts ? EXPECTED_MODULE_ARTIFACTS : EXPECTED_ARTIFACTS;
        for (String expectedArtifact : expectedArtifacts) {
            artifactoryManager.downloadHeaders(localRepo + ARTIFACTS_GROUP_ID + expectedArtifact);
        }

        // Check buildInfo info
        BuildInfo buildInfo = getBuildInfo(artifactoryManager, buildResult);
        assertNotNull(buildInfo);
        checkBuildInfoModules(buildInfo, 3, expectModuleArtifacts ? 5 : 4);
    }

    static void assertProjectsSuccess(BuildResult buildResult) {
        assertSuccess(buildResult, ":api:artifactoryPublish");
        assertSuccess(buildResult, ":shared:artifactoryPublish");
        assertSuccess(buildResult, ":services:webservice:artifactoryPublish");
        assertSuccess(buildResult, ":artifactoryPublish");
    }

    static void checkLocalBuild(BuildResult buildResult, File buildInfoJson, int expectedModules, int expectedArtifactsPerModule) throws IOException {
        assertProjectsSuccess(buildResult);

        // Assert build info contains requestedBy information.
        assertTrue(buildInfoJson.exists());
        BuildInfo buildInfo = jsonStringToBuildInfo(CommonUtils.readByCharset(buildInfoJson, StandardCharsets.UTF_8));
        checkBuildInfoModules(buildInfo, expectedModules, expectedArtifactsPerModule);
        assertRequestedBy(buildInfo);
    }

    private static void assertRequestedBy(BuildInfo buildInfo) {
        List<Dependency> apiDependencies = buildInfo.getModule("org.jfrog.test.gradle.publish:api:1.0-SNAPSHOT").getDependencies();
        assertEquals(apiDependencies.size(), 5);
        for (Dependency dependency : apiDependencies) {
            if (dependency.getId().equals("commons-io:commons-io:1.2")) {
                String[][] requestedBy = dependency.getRequestedBy();
                assertNotNull(requestedBy);
                assertEquals(requestedBy.length, 1);
                assertEquals(requestedBy[0].length, 2);
                assertEquals(requestedBy[0][0], "org.apache.commons:commons-lang3:3.12.0");
                assertEquals(requestedBy[0][1], "org.jfrog.test.gradle.publish:api:1.0-SNAPSHOT");
            }
        }
    }

    /**
     * Get build info from the build info URL.
     *
     * @param artifactoryManager - The ArtifactoryManager client
     * @param buildResult        - The build results
     * @return build info or null
     * @throws IOException - In case of any IO error
     */
    public static BuildInfo getBuildInfo(ArtifactoryManager artifactoryManager, BuildResult buildResult) throws IOException {
        Pair<String, String> buildDetails = getBuildDetails(buildResult);
        return artifactoryManager.getBuildInfo(buildDetails.getLeft(), buildDetails.getRight(), null);
    }

    /**
     * @param buildResult Details of the build
     * @return A pair of: Left item - buildResult's build name, Right item - buildResult's build number
     */
    public static Pair<String, String> getBuildDetails(BuildResult buildResult) {
        // Get build info URL
        String[] res = StringUtils.substringAfter(buildResult.getOutput(), BUILD_BROWSE_URL).split("/");
        assertTrue(ArrayUtils.getLength(res) >= 3, "Couldn't find build info URL link");

        // Extract build name and number from build info URL
        String buildName = res[1];
        String buildNumber = StringUtils.substringBefore(res[2], System.lineSeparator());
        return Pair.of(buildName, buildNumber);
    }

    /**
     * Check expected build info modules.
     *
     * @param buildInfo                  - The build info
     * @param expectedModules            - Number of expected modules.
     * @param expectedArtifactsPerModule - Number of expected artifacts in each module.
     */
    private static void checkBuildInfoModules(BuildInfo buildInfo, int expectedModules, int expectedArtifactsPerModule) {
        List<Module> modules = buildInfo.getModules();
        assertEquals(modules.size(), expectedModules);
        for (Module module : modules) {
            if (expectedArtifactsPerModule > 0) {
                assertEquals(module.getArtifacts().size(), expectedArtifactsPerModule);
            } else {
                assertNull(module.getArtifacts());
            }

            switch (module.getId()) {
                case "org.jfrog.test.gradle.publish:webservice:1.0-SNAPSHOT":
                    assertEquals(module.getDependencies().size(), 7);
                    break;
                case "org.jfrog.test.gradle.publish:api:1.0-SNAPSHOT":
                    assertEquals(module.getDependencies().size(), 5);
                    break;
                case "org.jfrog.test.gradle.publish:shared:1.0-SNAPSHOT":
                    assertEquals(module.getDependencies().size(), 0);
                    break;
                default:
                    fail("Unexpected module ID: " + module.getId());
            }
        }
    }

    /**
     * Check that the expected properties are found on each published artifact.
     *
     * @param artifactoryManager - ArtifactoryManager client
     */
    static void checkArtifactsProps(ArtifactoryManager artifactoryManager) throws IOException {
        // Test single value prop
        PropertySearchResult artifacts = artifactoryManager.searchArtifactsByProperties("gradle.test.single.value.key=basic");
        assertTrue(artifacts.getResults().size() >= 12);
        // Test multi value props
        artifacts = artifactoryManager.searchArtifactsByProperties("gradle.test.multi.values.key=val1");
        assertTrue(artifacts.getResults().size() >= 12);
        artifacts = artifactoryManager.searchArtifactsByProperties("gradle.test.multi.values.key=val2");
        assertTrue(artifacts.getResults().size() >= 12);
        artifacts = artifactoryManager.searchArtifactsByProperties("gradle.test.multi.values.key=val3");
        assertTrue(artifacts.getResults().size() >= 12);
    }

    /**
     * Assert build success for task.
     *
     * @param buildResult - The build results
     * @param taskName    - The task name
     */
    private static void assertSuccess(BuildResult buildResult, String taskName) {
        BuildTask buildTask = buildResult.task(taskName);
        assertNotNull(buildTask);
        assertEquals(buildTask.getOutcome(), SUCCESS);
    }

    /**
     * Generate Gradle init script.
     *
     * @throws IOException - In case of any IO error
     */
    private static void generateInitScript() throws IOException {
        String content = new String(Files.readAllBytes(INIT_SCRIPT), StandardCharsets.UTF_8);
        // Escape "/" in windows machines
        String libsDir = LIBS_DIR.toString().replaceAll("\\\\", "\\\\\\\\");
        content = content.replace("${pluginLibDir}", libsDir);
        Path target = TEST_DIR.toPath().resolve("gradle.init");
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }
}
