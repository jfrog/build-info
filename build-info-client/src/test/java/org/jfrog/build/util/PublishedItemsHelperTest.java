package org.jfrog.build.util;

import com.google.common.collect.Multimap;
import org.apache.commons.io.FilenameUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests {@link PublishedItemsHelper} for proper functionality
 *
 * @author Shay Yaakov
 */
@Test
public class PublishedItemsHelperTest {

    private File checkoutDir;
    private File absoluteFile;

    @BeforeClass
    public void setup() {
        checkoutDir = getResourceAsFile("/root/workspace/workspace2");
        absoluteFile = getResourceAsFile("/bla/settings.jar");
    }

    public void testDoubleDotWithStartWildcard() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs("../../saas/**/*.xml=>target/xml");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 2, "Expected to find 2 files");
            for (Map.Entry<String, File> fileEntry : buildPublishingData.entries()) {
                String targetPath = PublishedItemsHelper.calculateTargetPath(fileEntry.getKey(), fileEntry.getValue());
                assertTrue(targetPath.startsWith("target/xml/hello"),
                        "Expected target path to start with 'target/xml'");
            }
        }
    }

    public void testAbsolutePath() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs(
                absoluteFile.getAbsolutePath() + "=>jaja/gululu");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 1, "Expected to find 1 files");
            assertTrue(buildPublishingData.containsValue(absoluteFile), "Expected to find the absolute file");
            for (Map.Entry<String, File> fileEntry : buildPublishingData.entries()) {
                String targetPath = PublishedItemsHelper.calculateTargetPath(fileEntry.getKey(), fileEntry.getValue());
                assertTrue(targetPath.startsWith("jaja/gululu"), "Expected target path to start with 'jaja/gululu'");
            }
        }
    }

    public void testAbsolutePathSameWorkspace() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs(checkoutDir.getAbsolutePath() + "/inner/*.gradle" + "=>test/props");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 1, "Expected to find 1 file");
            for (Map.Entry<String, File> fileEntry : buildPublishingData.entries()) {
                String targetPath = PublishedItemsHelper.calculateTargetPath(fileEntry.getKey(), fileEntry.getValue());
                assertTrue(targetPath.startsWith("test/props"), "Expected target path to start with 'test/props'");
            }
        }
    }

    public void testMultiPatterns() throws IOException {
        String pattern = "**/multi1/*=>test/multi1, **multi2/*=>test/multi2";
        Multimap<String, String> pairs = getPublishedItemsPatternPairs(pattern);
        assertEquals(pairs.keySet().size(), 2, "Expected to find 2 keys");
    }

    public void testAllWorkspace() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs("**");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 6, "Expected to find 6 files");
        }
    }

    public void testAbsolutePathWithDoubleStar() throws IOException {
        File resourceAsFile = getResourceAsFile("/root/workspace");
        String fileAbsolutePath = FilenameUtils.separatorsToUnix(resourceAsFile.getAbsolutePath());
        Multimap<String, String> pairs = getPublishedItemsPatternPairs(fileAbsolutePath + "/ant/**");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 7, "Expected to find 7 files");
        }
    }

    public void testAllSpecificFilesFromCheckoutDir() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs("**/*.blabla=>blabla");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 2, "Expected to find 2 files");
            for (Map.Entry<String, File> fileEntry : buildPublishingData.entries()) {
                String targetPath = PublishedItemsHelper.calculateTargetPath(fileEntry.getKey(), fileEntry.getValue());
                assertTrue(targetPath.startsWith("blabla/inner"), "Expected target path to start with 'blabla'");
            }
        }
    }

    public void testEmptyTargetPath() throws IOException {
        Multimap<String, String> pairs = getPublishedItemsPatternPairs("../../**/**/*.xml");
        for (final Map.Entry<String, String> entry : pairs.entries()) {
            Multimap<String, File> buildPublishingData = getBuildPublishingData(entry);
            assertEquals(buildPublishingData.size(), 2, "Expected to find 2 files");
            for (Map.Entry<String, File> fileEntry : buildPublishingData.entries()) {
                String targetPath = PublishedItemsHelper.calculateTargetPath(fileEntry.getKey(), fileEntry.getValue());
                assertTrue(targetPath.startsWith("saas/hello"), "Expected target path to start with 'saas'");
            }
        }
    }

    private Multimap<String, String> getPublishedItemsPatternPairs(String pattern) {
        return PublishedItemsHelper.getPublishedItemsPatternPairs(pattern);
    }

    private Multimap<String, File> getBuildPublishingData(Map.Entry<String, String> entry) throws IOException {
        return PublishedItemsHelper.buildPublishingData(checkoutDir, entry.getKey(), entry.getValue());
    }

    private File getResourceAsFile(String path) {
        URL resource = this.getClass().getResource(path);
        assertResourceNotNull(path, resource);
        return new File(resource.getFile());
    }

    private void assertResourceNotNull(String resourcePath, Object resourceHandle) {
        if (resourceHandle == null) {
            throw new IllegalArgumentException("Could not find the classpath resource at: " + resourcePath + ".");
        }
    }
}
