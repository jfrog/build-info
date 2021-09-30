package org.jfrog.build.extractor.go;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.executor.CommandResults;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

/**
 * @author yahavi
 **/
public class GoDriverTest {
    private static final Path PROJECT_ORIGIN = Paths.get(".").toAbsolutePath().normalize()
            .resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor", "project1"));
    private static final Set<String> EXPECTED_USED_MODULES = Sets.newHashSet("github.com/jfrog/dependency",
            "rsc.io/sampler v1.3.0",
            "golang.org/x/text v0.0.0-20170915032832-14c0d48ead0c",
            "rsc.io/quote v1.5.2");

    @Test
    public void testListUsedModules() throws IOException {
        File projectDir = Files.createTempDirectory("").toFile();
        try {
            FileUtils.copyDirectory(PROJECT_ORIGIN.toFile(), projectDir);
            GoDriver driver = new GoDriver(null, System.getenv(), projectDir, new NullLog());
            driver.modTidy(false);
            CommandResults results = driver.getUsedModules(false);
            Set<String> actualUsedModules = Arrays.stream(results.getRes().split("\\r?\\n"))
                    .map(String::trim)
                    .collect(Collectors.toSet());
            assertEquals(actualUsedModules, EXPECTED_USED_MODULES);
        } finally {
            FileUtils.deleteDirectory(projectDir);
        }
    }
}
