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

    // Build info json path if not published
    static final Path BUILD_INFO_JSON = TEST_DIR.toPath().resolve(Paths.get("build", "build-info.json"));

    // Root directories
    static final Path GRADLE_EXTRACTOR = Paths.get(".").normalize().toAbsolutePath();
    static final Path GRADLE_EXTRACTOR_SRC = GRADLE_EXTRACTOR.resolve("src");
    static final Path PROJECTS_ROOT = GRADLE_EXTRACTOR_SRC.resolve(Paths.get("test", "resources", "integration"));

    // Projects
    static final Path GRADLE_EXAMPLE = PROJECTS_ROOT.resolve("gradle-example");
    static final Path GRADLE_EXAMPLE_PUBLISH = PROJECTS_ROOT.resolve("gradle-example-publish");
    static final Path GRADLE_KTS_EXAMPLE_PUBLISH = PROJECTS_ROOT.resolve("gradle-kts-example-publish");
    static final Path GRADLE_EXAMPLE_CI_SERVER = PROJECTS_ROOT.resolve("gradle-example-ci-server");
    static final Path DEPRECATED_GRADLE_EXAMPLE_CI_SERVER = PROJECTS_ROOT.resolve("gradle-example-ci-server-deprecated");

    // CI example paths
    static final Path LIBS_DIR = GRADLE_EXTRACTOR.resolve(Paths.get("build", "libs"));
    static final Path INIT_SCRIPT = GRADLE_EXTRACTOR_SRC.resolve(Paths.get("main", "resources", "initscripttemplate.gradle"));
    static final Path BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER = PROJECTS_ROOT.resolve("buildinfo.properties.deployer");
    static final Path BUILD_INFO_PROPERTIES_SOURCE_RESOLVER = PROJECTS_ROOT.resolve("buildinfo.properties.resolver");
    static final Path DEPRECATED_BUILD_INFO_PROPERTIES_SOURCE_DEPLOYER = PROJECTS_ROOT.resolve("deprecated.buildinfo.properties.deployer");
    static final Path DEPRECATED_BUILD_INFO_PROPERTIES_SOURCE_RESOLVER = PROJECTS_ROOT.resolve("deprecated.buildinfo.properties.resolver");
    static final Path BUILD_INFO_PROPERTIES_TARGET = TEST_DIR.toPath().resolve("buildinfo.properties");

    // Expected artifacts
    static final String ARTIFACTS_GROUP_ID = "/org/jfrog/test/gradle/publish/";
    static final String[] EXPECTED_ARTIFACTS = {
            "api/ivy-1.0-SNAPSHOT.xml",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.jar",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.pom",
            "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.properties",
            "shared/ivy-1.0-SNAPSHOT.xml",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.jar",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.pom",
            "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.properties",
            "webservice/ivy-1.0-SNAPSHOT.xml",
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.jar",
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.pom",
            "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.properties"
    };
    static final String[] EXPECTED_MODULE_ARTIFACTS = Stream.concat(
            Stream.of(EXPECTED_ARTIFACTS),
            Stream.of(
                    "api/1.0-SNAPSHOT/api-1.0-SNAPSHOT.module",
                    "shared/1.0-SNAPSHOT/shared-1.0-SNAPSHOT.module",
                    "webservice/1.0-SNAPSHOT/webservice-1.0-SNAPSHOT.module")).
            toArray(String[]::new);
}
