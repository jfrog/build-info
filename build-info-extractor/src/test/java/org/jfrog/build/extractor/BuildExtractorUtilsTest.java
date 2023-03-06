package org.jfrog.build.extractor;

import org.apache.commons.lang3.ArrayUtils;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.*;
import org.jfrog.build.extractor.ci.Module;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.*;
import static org.testng.Assert.*;

/**
 * Test the build info extractor
 *
 * @author Tomer Cohen
 */
@Test
public class BuildExtractorUtilsTest {
    private static final String POPO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "popo";
    private static final String MOMO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "momo";
    private static final String ENV_POPO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "popo";
    private static final String ENV_MOMO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "momo";

    private Path tempFile;

    @BeforeMethod
    private void setUp() throws IOException {
        tempFile = Files.createTempFile("BuildInfoExtractorUtilsTest", "").toAbsolutePath();
    }

    @AfterMethod
    private void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
    }

    public void getBuildInfoPropertiesFromSystemProps() {
        System.setProperty(POPO_KEY, "buildname");
        System.setProperty(MOMO_KEY, "1");

        Properties props = filterDynamicProperties(mergePropertiesWithSystemAndPropertyFile(new Properties()), BUILD_INFO_PROP_PREDICATE);

        assertEquals(props.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(props.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(props.getProperty(MOMO_KEY), "1", "momo property does not match");
        System.clearProperty(POPO_KEY);
        System.clearProperty(MOMO_KEY);
    }

    public void getBuildInfoPropertiesFromFile() throws IOException {
        Properties props = new Properties();
        props.put(POPO_KEY, "buildname");
        props.put(MOMO_KEY, "1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            props.store(fileOutputStream, "");
        }

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        Properties fileProps = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties()),
                BUILD_INFO_PROP_PREDICATE);

        assertEquals(fileProps.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(MOMO_KEY), "1", "momo property does not match");

        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
    }

    public void getBuildInfoProperties() throws IOException {
        Properties props = new Properties();
        props.put(POPO_KEY, "buildname");
        props.put(MOMO_KEY, "1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            props.store(fileOutputStream, "");
        }
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        // Put system properties
        String kokoKey = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "koko";
        String gogoKey = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "2");

        Properties buildInfoProperties = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties()),
                BUILD_INFO_PROP_PREDICATE);

        assertEquals(buildInfoProperties.size(), 4, "There should be 4 properties");
        assertEquals(buildInfoProperties.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty(kokoKey), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty(gogoKey), "2", "gogo parent number property does not match");

        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void getEnvPropertiesFromFile() throws IOException {
        Properties props = new Properties();
        props.put(ENV_POPO_KEY, "buildname");
        props.put(ENV_MOMO_KEY, "1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            props.store(fileOutputStream, "");
        }
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        Properties fileProps = getEnvProperties(new Properties(), null);
        assertEquals(fileProps.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(ENV_MOMO_KEY), "1", "momo property does not match");

        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
    }

    public void getEnvAndSysPropertiesFromFile() throws IOException {
        Properties props = new Properties();
        props.put(ENV_POPO_KEY, "buildname");
        props.put(ENV_MOMO_KEY, "1");
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            props.store(fileOutputStream, "");
        }
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        // Put system properties
        String kokoKey = "koko";
        String gogoKey = "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "2");

        Properties buildInfoProperties = getEnvProperties(new Properties(), null);
        assertEquals(buildInfoProperties.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(ENV_MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty("koko"), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty("gogo"), "2", "gogo parent number property does not match");

        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void testExcludePatterns() {
        // Put system properties
        String kokoKey = "koko";
        String koko2Key = "akoko";
        String gogoKey = "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(koko2Key, "parent2");
        System.setProperty(gogoKey, "2");

        Properties startProps = new Properties();
        startProps.put(BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS, "*koko");
        Properties buildInfoProperties = getEnvProperties(startProps, null);
        assertNull(buildInfoProperties.getProperty("koko"), "Should not find koko property due to exclude patterns");
        assertNull(buildInfoProperties.getProperty("akoko"), "Should not find akoko property due to exclude patterns");
        assertEquals(buildInfoProperties.getProperty("gogo"), "2", "gogo parent number property does not match");

        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void testIncludePatterns() {
        // Put system properties
        String gogoKey = "gogo1";
        String gogo2Key = "gogo2a";
        String kokoKey = "koko";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "1");
        System.setProperty(gogo2Key, "2");

        Properties startProps = new Properties();
        startProps.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "gogo?*");
        Properties buildInfoProperties = getEnvProperties(startProps, null);
        assertEquals(buildInfoProperties.getProperty("gogo1"), "1", "gogo1 parent number property does not match");
        assertEquals(buildInfoProperties.getProperty("gogo2a"), "2", "gogo2a parent number property does not match");
        assertNull(buildInfoProperties.getProperty("koko"), "Should not find koko property due to include patterns");

        System.clearProperty(gogoKey);
        System.clearProperty(gogo2Key);
        System.clearProperty(kokoKey);
    }

    public void testBuildToJson() throws IOException {
        String[] requestedByA = new String[]{"parentA", "b", "moduleId"};
        String[] requestedByB = new String[]{"parentB", "d", "moduleId"};
        Dependency dependencyA = new DependencyBuilder().id("depA").addRequestedBy(requestedByA).addRequestedBy(requestedByB).build();
        Module module = new ModuleBuilder().id("moduleId").addDependency(dependencyA).build();
        BuildInfo buildInfo = new BuildInfoBuilder("buildId").number("12").started("34").addModule(module).build();

        // Serialize and deserialize again
        BuildInfo actualBuildInfo = jsonStringToBuildInfo(buildInfoToJsonString(buildInfo));

        // Check buildInfo
        assertEquals(actualBuildInfo.getName(), buildInfo.getName());
        assertEquals(actualBuildInfo.getNumber(), buildInfo.getNumber());
        assertEquals(actualBuildInfo.getStarted(), buildInfo.getStarted());

        // Check module
        Module actualModule = actualBuildInfo.getModule(module.getId());
        assertNotNull(actualModule);

        // Check dependency
        assertEquals(actualModule.getDependencies().size(), 1);
        Dependency actualDependency = actualModule.getDependencies().get(0);
        assertEquals(actualDependency.getId(), dependencyA.getId());

        // Check requestedBy
        String[][] requestedBy = actualDependency.getRequestedBy();
        assertEquals(ArrayUtils.getLength(requestedBy), 2);
        assertEquals(requestedBy[0], requestedByA);
        assertEquals(requestedBy[1], requestedByB);
    }

    @DataProvider
    private Object[][] buildUrlProvider() {
        return new Object[][]{
                // Platform URL - encoding
                {"http://127.0.0.1", "na me", "1 2", "123456", "", true, true, "http://127.0.0.1/ui/builds/na%20me/1%202/123456/published"},
                {"http://127.0.0.1", "na me", "1 2", "123456", "proj", true, true, "http://127.0.0.1/ui/builds/na%20me/1%202/123456/published?buildRepo=proj-build-info&projectKey=proj"},
                {"http://127.0.0.1", "na me", "1 2", "", "", true, true, "http://127.0.0.1/ui/builds/na%20me/1%202/published"},

                // Platform URL - no encoding
                {"http://127.0.0.1", "na me", "1 2", "123456", "", false, true, "http://127.0.0.1/ui/builds/na me/1 2/123456/published"},
                {"http://127.0.0.1", "na me", "1 2", "123456", "proj", false, true, "http://127.0.0.1/ui/builds/na me/1 2/123456/published?buildRepo=proj-build-info&projectKey=proj"},
                {"http://127.0.0.1", "na me", "1 2", "", "", false, true, "http://127.0.0.1/ui/builds/na me/1 2/published"},

                // Artifactory URL - encoding
                {"http://127.0.0.1/artifactory", "na me", "1 2", "123456", "", true, false, "http://127.0.0.1/artifactory/webapp/builds/na%20me/1%202"},
                {"http://127.0.0.1/artifactory", "na me", "1 2", "123456", "proj", true, false, "http://127.0.0.1/ui/builds/na%20me/1%202/123456/published?buildRepo=proj-build-info&projectKey=proj"},
                {"http://127.0.0.1/artifactory", "na me", "1 2", "", "", true, false, "http://127.0.0.1/artifactory/webapp/builds/na%20me/1%202"},
                {"http://127.0.0.1/non-artifactory", "na me", "1 2", "123456", "", true, false, "http://127.0.0.1/non-artifactory/webapp/builds/na%20me/1%202"},
                {"http://127.0.0.1/non-artifactory", "na me", "1 2", "123456", "proj", true, false, ""},

                // Artifactory URL - no encoding
                {"http://127.0.0.1/artifactory", "na me", "1 2", "123456", "", false, false, "http://127.0.0.1/artifactory/webapp/builds/na me/1 2"},
                {"http://127.0.0.1/artifactory", "na me", "1 2", "123456", "proj", false, false, "http://127.0.0.1/ui/builds/na me/1 2/123456/published?buildRepo=proj-build-info&projectKey=proj"},
                {"http://127.0.0.1/artifactory", "na me", "1 2", "", "", false, false, "http://127.0.0.1/artifactory/webapp/builds/na me/1 2"},
                {"http://127.0.0.1/non-artifactory", "na me", "1 2", "123456", "", false, false, "http://127.0.0.1/non-artifactory/webapp/builds/na me/1 2"},
                {"http://127.0.0.1/non-artifactory", "na me", "1 2", "123456", "proj", false, false, ""},
        };
    }


    @Test(dataProvider = "buildUrlProvider")
    public void testCreateBuildInfoUrl(String url, String buildName, String buildNumber, String timeStamp, String project,
                                       boolean encode, boolean platformUrl, String exceptedUrl) {
        assertEquals(createBuildInfoUrl(url, buildName, buildNumber, timeStamp, project, encode, platformUrl), exceptedUrl);
    }
}
