package org.jfrog.build.extractor.go;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandResults;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class GoDriverTest {
    private static final Path BASE_PATH = Paths.get(".").toAbsolutePath().normalize()
            .resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));
    private static final Path PROJECT_1 = BASE_PATH.resolve("project1");
    private static final Path ERRONEOUS = BASE_PATH.resolve("project1-err");
    private static final Set<String> EXPECTED_USED_MODULES = Sets.newHashSet("github.com/jfrog/dependency",
            "rsc.io/sampler v1.3.0",
            "golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c",
            "rsc.io/quote v1.5.2");

    /**
     * Test "go mod tidy" and "go list".
     *
     * @throws IOException in case of any I/O exception.
     */
    @Test
    public void testTidyListUsedModules() throws IOException {
        File projectDir = Files.createTempDirectory("").toFile();
        try {
            FileUtils.copyDirectory(PROJECT_1.toFile(), projectDir);
            GoDriver driver = new GoDriver(null, System.getenv(), projectDir, new NullLog());
            driver.modTidy(false, false);

            // Run "go list -f {{with .Module}}{{.Path}} {{.Version}}{{end}} all"
            CommandResults results = driver.getUsedModules(false, false, true);
            Set<String> actualUsedModules = Arrays.stream(results.getRes().split("\\r?\\n"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            assertEquals(actualUsedModules, EXPECTED_USED_MODULES);

            // Run "go list -e -f {{with .Module}}{{.Path}} {{.Version}}{{end}} all"
            results = driver.getUsedModules(false, true, true);
            actualUsedModules = Arrays.stream(results.getRes().split("\\r?\\n"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            assertEquals(actualUsedModules, EXPECTED_USED_MODULES);
        } finally {
            FileUtils.deleteDirectory(projectDir);
        }
    }

    /**
     * Test "go mod tidy" and "go list" on a project with errors.
     *
     * @throws IOException in case of any I/O exception.
     */
    @Test
    public void testTidyListUsedModulesErroneous() throws IOException {
        File projectDir = Files.createTempDirectory("").toFile();
        try {
            FileUtils.copyDirectory(ERRONEOUS.toFile(), projectDir);
            GoDriver driver = new GoDriver(null, System.getenv(), projectDir, new NullLog());
            Assert.assertThrows(() -> driver.modTidy(false, false));
            driver.modTidy(false, true);

            // Run "go list -f {{with .Module}}{{.Path}} {{.Version}}{{end}} all"
            Assert.assertThrows(() -> driver.getUsedModules(false, false, true));

            // Run "go list -e -f {{with .Module}}{{.Path}} {{.Version}}{{end}} all"
            CommandResults results = driver.getUsedModules(false, true, true);
            Set<String> actualUsedModules = Arrays.stream(results.getRes().split("\\r?\\n"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            assertEquals(actualUsedModules, EXPECTED_USED_MODULES);
        } finally {
            FileUtils.deleteDirectory(projectDir);
        }
    }

    /**
     * Test "go get".
     *
     * @throws IOException in case of any I/O exception.
     */
    @Test
    public void testGoGet() throws IOException {
        File projectDir = Files.createTempDirectory("").toFile();
        try {
            FileUtils.copyDirectory(PROJECT_1.toFile(), projectDir);
            GoDriver driver = new GoDriver(null, System.getenv(), projectDir, new NullLog());
            driver.modTidy(false, true);
            // Get dependency and expect it later on go modules list
            driver.get("rsc.io/sampler@v1.3.1", false);

            // Run "go list -f {{with .Module}}{{.Path}} {{.Version}}{{end}} all"
            CommandResults results = driver.getUsedModules(false, false, false);
            Set<String> actualUsedModules = Arrays.stream(results.getRes().split("\\r?\\n")).map(String::trim).collect(Collectors.toSet());
            assertTrue(actualUsedModules.contains("rsc.io/sampler v1.3.1"));
        } finally {
            FileUtils.deleteDirectory(projectDir);
        }
    }

    private static String getPathEnv(Map<String, String> env) {
        return env.get(SystemUtils.IS_OS_WINDOWS ? "Path" : "PATH");
    }

    @Test
    public void testGoDriverInit() throws IOException {
        File projectDir = Files.createTempDirectory("").toFile();
        try {
            if (SystemUtils.IS_OS_WINDOWS) {
                Map<String, String> env = System.getenv();
                new GoDriver("C:\\Program Files\\Go\\bin\\go", env, projectDir, new NullLog());
                assertTrue(getPathEnv(env).contains("C:\\Program Files\\Go\\bin"));
            }
        } finally {
            FileUtils.deleteDirectory(projectDir);
        }
    }
}
