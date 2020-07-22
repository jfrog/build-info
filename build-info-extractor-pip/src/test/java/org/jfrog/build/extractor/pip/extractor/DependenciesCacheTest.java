package org.jfrog.build.extractor.pip.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Created by Bar Belity on 21/07/2020.
 */
public class DependenciesCacheTest {

    static final Log log = new TestingLog();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void dependenciesCacheTest() {
        Map<String, Dependency> cacheMap = new HashMap<>();
        // Create depA.
        Dependency depA = new DependencyBuilder().id("depenA-1.0-A.zip").sha1("sha1A").md5("md5A").build();
        cacheMap.put("depA", depA);
        // Create depC.
        Dependency depC = new DependencyBuilder().id("depenC-3.4-C.gzip").sha1("sha1C").md5("md5C").build();
        cacheMap.put("depC", depC);

        // Test cache methods.
        Path projectDir = null;
        try {
            projectDir = TestUtils.createTempDir("pipCacheTest");
            // Test reading empty cache.
            DependenciesCache nullCache = DependenciesCache.getProjectDependenciesCache(projectDir, log);
            assertNull(nullCache, "Expecting null cache read.");
            // Test writing cache.
            DependenciesCache.updateDependenciesCache(cacheMap, projectDir);
            // Test reading cache.
            DependenciesCache readCache = DependenciesCache.getProjectDependenciesCache(projectDir, log);
            assertNotNull(readCache, "Expecting cache read.");
            // Validate cache content.
            Dependency depATest = readCache.getDependency("depA");
            assertEquals(depATest, depA, String.format("Actual: %s\nExpected: %s", mapper.writeValueAsString(depATest), mapper.writeValueAsString(depA)));
            Dependency depCTest = readCache.getDependency("depC");
            assertEquals(depCTest, depC, String.format("Actual: %s\nExpected: %s", mapper.writeValueAsString(depCTest), mapper.writeValueAsString(depC)));
            Dependency nonExistingDepTTest = readCache.getDependency("depT");
            assertNull(nonExistingDepTTest, String.format("Actual: %s\nExpected null", mapper.writeValueAsString(nonExistingDepTTest)));
        } catch (IOException e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }
}
