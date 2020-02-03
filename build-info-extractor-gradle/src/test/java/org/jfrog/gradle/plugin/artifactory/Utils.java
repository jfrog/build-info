package org.jfrog.gradle.plugin.artifactory;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.BuildTask;
import org.gradle.testkit.runner.GradleRunner;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.jfrog.gradle.plugin.artifactory.Consts.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

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
     * @param contextUrl  - Artifactory URL
     * @param username    - Artifactory username
     * @param password    - Artifactory password
     * @param localRepo   - Gradle local repository
     * @param virtualRepo - Gradle remote repository
     * @throws IOException - In case of any IO error
     */
    static void generateBuildInfoProperties(String contextUrl, String username, String password, String localRepo, String virtualRepo) throws IOException {
        String content = new String(Files.readAllBytes(BUILD_INFO_PROPERTIES_SOURCE), StandardCharsets.UTF_8);
        Map<String, String> valuesMap = new HashMap<String, String>() {{
            put("contextUrl", contextUrl);
            put("username", username);
            put("password", password);
            put("localRepo", localRepo);
            put("virtualRepo", virtualRepo);
        }};
        StrSubstitutor sub = new StrSubstitutor(valuesMap);
        content = sub.replace(content);
        Files.write(BUILD_INFO_PROPERTIES_TARGET, content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 1. Check the build status of all tasks.
     * 2. Make sure all artifacts deployed.
     *
     * @param buildResult           - The build results
     * @param expectModuleArtifacts - Should we expect *.module files
     * @param dependenciesClient    - Artifactory dependencies client
     * @param url                   - Artifactory URL
     * @param localRepo             - Artifactory local localRepo
     * @throws IOException - In case of any IO error
     */
    static void checkBuildResults(BuildResult buildResult, boolean expectModuleArtifacts, ArtifactoryDependenciesClient dependenciesClient, String url, String localRepo) throws IOException {
        // Assert all tasks ended with success outcome
        assertSuccess(buildResult, ":api:artifactoryPublish");
        assertSuccess(buildResult, ":shared:artifactoryPublish");
        assertSuccess(buildResult, ":services:webservice:artifactoryPublish");
        assertSuccess(buildResult, ":artifactoryPublish");

        // Check that all expected artifacts uploaded to Artifactory
        String[] expectedArtifacts = expectModuleArtifacts ? EXPECTED_MODULE_ARTIFACTS : EXPECTED_ARTIFACTS;
        for (String expectedArtifact : expectedArtifacts) {
            dependenciesClient.getArtifactMetadata(url + localRepo + ARTIFACTS_GROUP_ID + expectedArtifact);
        }
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
        content = content.replace("${pluginLibDir}", LIBS_DIR.toString());
        Path target = TEST_DIR.toPath().resolve("gradle.init");
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }
}
