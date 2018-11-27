package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Module;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

@Test
public class NpmExtractorTest extends IntegrationTestsBase {

    private static final String NPM_LOCAL_REPO = "build-info-tests-npm-local";
    private static final String NPM_REMOTE_REPO = "build-info-tests-npm-remote";
    private static final String NPM_VIRTUAL_REPO = "build-info-tests-npm-virtual";

    private static final Path PROJECTS_PATH = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));
    private static final File PROJECT_A = PROJECTS_PATH.resolve("a").toFile();
    private static final File PROJECT_B = PROJECTS_PATH.resolve("b").toFile();
    private static final File PROJECT_C = PROJECTS_PATH.resolve("c").toFile();

    private File projectA, projectB, projectC;

    private static final String PACKAGE_A_NAME = "package-name1";
    private static final String PACKAGE_A_VERSION = "0.0.1";
    private static final String PACKAGE_B_NAME = "package-name2";
    private static final String PACKAGE_B_VERSION = "0.0.2";
    private static final String PACKAGE_C_NAME = "package-name3";
    private static final String PACKAGE_C_VERSION = "0.0.3";
    private static final List<String> PROJECT_A_DEPENDENCIES = Lists.newArrayList("debug-3.1.0.tgz", "ms-2.0.0.tgz");
    private static final List<String> PROJECT_B_DEPENDENCIES = Lists.newArrayList("debug-3.1.0.tgz", "ms-2.0.0.tgz", "fresh-0.1.0.tgz", "mime-1.2.6.tgz", "range-parser-0.0.4.tgz", "send-0.1.0.tgz");
    private static final Set<String> PROJECT_C_DEPENDENCIES = Stream.concat(PROJECT_A_DEPENDENCIES.stream(), PROJECT_B_DEPENDENCIES.stream()).collect(Collectors.toSet());

    public NpmExtractorTest() {
        localRepo = NPM_LOCAL_REPO;
        remoteRepo = NPM_REMOTE_REPO;
        virtualRepo = NPM_VIRTUAL_REPO;
    }

    @BeforeMethod
    public void setUp() {
        try {
            projectA = Files.createTempDirectory("NpmExtractorTest Project A").toFile();
            projectB = Files.createTempDirectory("NpmExtractorTest-Project-B").toFile();
            projectC = Files.createTempDirectory("NpmExtractorTestProjectC").toFile();
            FileUtils.copyDirectory(PROJECT_A, projectA);
            FileUtils.copyDirectory(PROJECT_B, projectB);
            FileUtils.copyDirectory(PROJECT_C, projectC);
            projectA.deleteOnExit();
            projectB.deleteOnExit();
            projectC.deleteOnExit();
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @AfterMethod
    public void tearDown() {
        FileUtils.deleteQuietly(projectA);
        FileUtils.deleteQuietly(projectB);
        FileUtils.deleteQuietly(projectC);
    }

    @Test
    public void testNpmInstallA() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, null, null, log, projectA);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), PACKAGE_A_NAME + ":" + PACKAGE_A_VERSION);
            assertModuleContained(module, PROJECT_A_DEPENDENCIES);
            assertModuleContains(module, PROJECT_A_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallDevA() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--only=dev", null, log, projectA);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "development:" + PACKAGE_A_NAME + ":" + PACKAGE_A_VERSION);
            assertTrue(module.getDependencies().isEmpty());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallProdA() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--only=prod", null, log, projectA);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "production:" + PACKAGE_A_NAME + ":" + PACKAGE_A_VERSION);
            assertModuleContained(module, PROJECT_A_DEPENDENCIES);
            assertModuleContains(module, PROJECT_A_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallB() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, null, null, log, projectB);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), PACKAGE_B_NAME + ":" + PACKAGE_B_VERSION);
            assertModuleContained(module, PROJECT_B_DEPENDENCIES);
            assertModuleContains(module, PROJECT_B_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallDevB() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--only=dev", null, log, projectB);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "development:" + PACKAGE_B_NAME + ":" + PACKAGE_B_VERSION);
            assertModuleContained(module, PROJECT_B_DEPENDENCIES);
            assertModuleContains(module, PROJECT_B_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallProdB() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--production", null, log, projectB);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "production:" + PACKAGE_B_NAME + ":" + PACKAGE_B_VERSION);
            assertTrue(module.getDependencies().isEmpty());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallC() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, null, null, log, projectC);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), PACKAGE_C_NAME + ":" + PACKAGE_C_VERSION);
            assertModuleContained(module, PROJECT_C_DEPENDENCIES);
            assertModuleContains(module, PROJECT_C_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallDevC() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--only=development", null, log, projectC);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "development:" + PACKAGE_C_NAME + ":" + PACKAGE_C_VERSION);
            assertModuleContained(module, PROJECT_B_DEPENDENCIES);
            assertModuleContains(module, PROJECT_B_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testNpmInstallProdC() {
        NpmInstall npmInstall = new NpmInstall(dependenciesClient, NPM_VIRTUAL_REPO, "--only=production", null, log, projectC);
        try {
            Module module = npmInstall.execute();
            assertEquals(module.getId(), "production:" + PACKAGE_C_NAME + ":" + PACKAGE_C_VERSION);
            assertModuleContained(module, PROJECT_A_DEPENDENCIES);
            assertModuleContains(module, PROJECT_A_DEPENDENCIES);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    private void assertModuleContained(Module module, Collection<String> dependencies) {
        module.getDependencies().forEach(dependency ->
                assertTrue(dependencies.contains(dependency.getId()), "Dependency " + dependency.getId() + " must be contained in module"));
    }

    private void assertModuleContains(Module module, Collection<String> dependencies) {
        dependencies.forEach(dependency -> assertTrue(module.getDependencies().stream().anyMatch(actualDependency ->
                dependency.equals(actualDependency.getId())), "Dependency " + dependency + " must not be contained in module"));
    }
}