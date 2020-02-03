package org.jfrog.gradle.plugin.artifactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * @author yahavi
 */
public class Consts {
    // Repositories
    static final String GRADLE_LOCAL_REPO = "build-info-tests-gradle-local";
    static final String GRADLE_REMOTE_REPO = "build-info-tests-gradle-remote";
    static final String GRADLE_VIRTUAL_REPO = "build-info-tests-gradle-virtual";

    // The test directory
    static final File TEST_DIR = new File(System.getProperty("java.io.tmpdir"), "gradle_tests_space");

    // Root directories
    static final Path GRADLE_EXTRACTOR = java.nio.file.Paths.get(".").normalize().toAbsolutePath();
    static final Path GRADLE_EXTRACTOR_SRC = GRADLE_EXTRACTOR.resolve("src");
    static final Path PROJECTS_ROOT = GRADLE_EXTRACTOR_SRC.resolve(java.nio.file.Paths.get("test", "resources", "integration"));

    // Projects
    static final Path GRADLE_EXAMPLE = PROJECTS_ROOT.resolve("gradle-example");
    static final Path GRADLE_EXAMPLE_PUBLISH = PROJECTS_ROOT.resolve("gradle-example-publish");
    static final Path GRADLE_EXAMPLE_CI_SERVER = PROJECTS_ROOT.resolve("gradle-example-ci-server");

    // CI example paths
    static final Path LIBS_DIR = GRADLE_EXTRACTOR.resolve(Paths.get("build", "libs"));
    static final Path INIT_SCRIPT = GRADLE_EXTRACTOR_SRC.resolve(Paths.get("main", "resources", "initscripttemplate.gradle"));
    static final Path BUILD_INFO_PROPERTIES_SOURCE = PROJECTS_ROOT.resolve("buildinfo.properties");
    static final Path BUILD_INFO_PROPERTIES_TARGET = TEST_DIR.toPath().resolve("buildinfo.properties");

    // Expected artifacts
    static final String ARTIFACTS_GROUP_ID = "/org/jfrog/test/gradle/publish/";
    static final String[] EXPECTED_ARTIFACTS = {
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.jar",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.jar",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.jar",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.properties",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.properties",
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.properties",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.pom",
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.pom",
            "api/ivy-1.0-SNAPSHOT.xml",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.pom"
    };
    static final String[] EXPECTED_MODULE_ARTIFACTS = Stream.concat(
            Stream.of(EXPECTED_ARTIFACTS),
            Stream.of(
                    "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.module",
                    "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.module",
                    "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.module")).
            toArray(String[]::new);
}
