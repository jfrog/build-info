package org.jfrog.build.extractor;

import org.apache.commons.lang3.ArrayUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.ci.BuildInfoProperties;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.IntegrationTestsBase.getLog;
import static org.jfrog.build.api.BuildInfoConfigProperties.ENV_PROPERTIES_FILE_KEY;
import static org.jfrog.build.api.BuildInfoConfigProperties.ENV_PROPERTIES_FILE_KEY_IV;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.buildInfoToJsonString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createBuildInfoUrl;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.filterDynamicProperties;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getEnvProperties;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.isWindows;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.jsonStringToBuildInfo;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * Test the build info extractor
 *
 * @author Tomer Cohen
 */
@Test
public class BuildExtractorUtilsTest {
    private static final String POPO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "popo";
    private static final String MOMO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "momo";
    private static final String KOKO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "koko";
    private static final String GOGO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "gogo";
    private static final String ENV_POPO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "popo";
    private static final String ENV_MOMO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "momo";

    private Path tempFile;

    @BeforeMethod
    private void setUp() throws IOException {
        tempFile = Files.createTempFile("BuildInfoExtractorUtilsTest", "").toAbsolutePath();
    }

    public void getBuildInfoPropertiesFromSystemProps() {
        System.setProperty(POPO_KEY, "buildname");
        System.setProperty(MOMO_KEY, "1");

        Properties props = filterDynamicProperties(mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()), BUILD_INFO_PROP_PREDICATE);

        assertEquals(props.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(props.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(props.getProperty(MOMO_KEY), "1", "momo property does not match");
    }

    public void getBuildInfoPropertiesFromFile() throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            createProperties().store(fileOutputStream, "");
        }

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        Properties fileProps = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()),
                BUILD_INFO_PROP_PREDICATE);

        assertEquals(fileProps.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(MOMO_KEY), "1", "momo property does not match");
    }

    public void getBuildInfoPropertiesFromFileAndSystemProps() throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            createProperties().store(fileOutputStream, "");
        }

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());
        System.setProperty(KOKO_KEY, "3");
        // Override MOMO_KEY from file
        System.setProperty(MOMO_KEY, "2");

        Properties fileProps = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()),
                BUILD_INFO_PROP_PREDICATE);

        assertEquals(fileProps.size(), 3, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(MOMO_KEY), "2", "momo property does not match");
        assertEquals(fileProps.getProperty(KOKO_KEY), "3", "koko property does not match");
    }

    @AfterMethod
    private void tearDown() throws Exception {
        Files.deleteIfExists(tempFile);

        Arrays.asList(POPO_KEY, MOMO_KEY, KOKO_KEY, GOGO_KEY).forEach(System::clearProperty);
        unsetEnv(BuildInfoConfigProperties.PROP_PROPS_FILE);
        unsetEnv(ENV_PROPERTIES_FILE_KEY);
        unsetEnv(ENV_PROPERTIES_FILE_KEY_IV);
    }

    public void getBuildInfoProperties() throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            createProperties().store(fileOutputStream, "");
        }
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        // Put system properties
        System.setProperty(KOKO_KEY, "parent");
        System.setProperty(GOGO_KEY, "2");

        Properties buildInfoProperties = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()),
                BUILD_INFO_PROP_PREDICATE);

        assertEquals(buildInfoProperties.size(), 4, "There should be 4 properties");
        assertEquals(buildInfoProperties.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty(KOKO_KEY), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty(GOGO_KEY), "2", "gogo parent number property does not match");
    }

    public void getEnvPropertiesFromFile() throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            createPropertiesEnvs().store(fileOutputStream, "");
        }
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());

        Properties fileProps = getEnvProperties(new Properties(), null);
        assertEquals(fileProps.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(ENV_MOMO_KEY), "1", "momo property does not match");
    }

    public void getEnvAndSysPropertiesFromFile() throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            createPropertiesEnvs().store(fileOutputStream, "");
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

        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void getBuildInfoPropertiesFromEncryptedFile() throws Exception {
        setupEncryptedFileTest(createProperties());

        Properties fileProps = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()),
                BUILD_INFO_PROP_PREDICATE);
        assertEquals(fileProps.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(MOMO_KEY), "1", "momo property does not match");
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
        Properties buildInfoProperties = getEnvProperties(startProps, new NullLog());
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
        Properties buildInfoProperties = getEnvProperties(startProps, new NullLog());
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

    private Properties createProperties() {
        Properties props = new Properties();
        props.put(POPO_KEY, "buildname");
        props.put(MOMO_KEY, "1");
        return props;
    }

    private Properties createPropertiesEnvs() {
        Properties props = new Properties();
        props.put(ENV_POPO_KEY, "buildname");
        props.put(ENV_MOMO_KEY, "1");
        return props;
    }

    public void failToReadEncryptedFileWithNoKey() throws Exception {
        // Create encrypted file with properties
        setupEncryptedFileTest(createProperties());
        // Remove key
        unsetEnv(ENV_PROPERTIES_FILE_KEY);
        // Read properties from the encrypted file
        Properties fileProps = filterDynamicProperties(
                mergePropertiesWithSystemAndPropertyFile(new Properties(), getLog()),
                BUILD_INFO_PROP_PREDICATE);
        // Check if no properties are read
        assertEquals(fileProps.size(), 0, "0 properties should be present, the file is encrypted, and the key is not available");
    }

    private void setupEncryptedFileTest(Properties props) throws Exception {
        props.put(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());
        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, tempFile.toString());
        ArtifactoryClientConfiguration client = new ArtifactoryClientConfiguration(new NullLog());
        client.fillFromProperties(props);

        try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
            EncryptionKeyPair keyPair = client.persistToEncryptedPropertiesFile(fileOutputStream);
            setEnv(ENV_PROPERTIES_FILE_KEY, Base64.getEncoder().encodeToString(keyPair.getSecretKey()));
            setEnv(ENV_PROPERTIES_FILE_KEY_IV, Base64.getEncoder().encodeToString(keyPair.getIv()));
        }
    }

    public void getEnvAndSysPropertiesFromEncryptedFile() throws Exception {
        // Put system properties
        String kokoKey = "koko";
        String gogoKey = "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "2");

        // Encrypt properties and write to the file
        setupEncryptedFileTest(createPropertiesEnvs());

        // Read properties from the encrypted file
        Properties buildInfoProperties = getEnvProperties(new Properties(), new NullLog());

        // Check if decrypted properties are as expected
        assertEquals(buildInfoProperties.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(ENV_MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty("koko"), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty("gogo"), "2", "gogo parent number property does not match");

        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    private void setEnv(String key, String value) throws Exception {
        modifyEnv(key, value);
    }

    private void unsetEnv(String key) throws Exception {
        modifyEnv(key, "");
    }

    /**
     * Modifies (set/unset) environment variables using reflection
     */
    @SuppressWarnings("unchecked")
    private static void modifyEnv(String key, String newValue) throws Exception {
        if (isWindows()) {
            System.setProperty(key, newValue);
            return;
        }
        Map<String, String> env = System.getenv();
        Class<?> cl = env.getClass();
        Field field = cl.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writableEnv = (Map<String, String>) field.get(env);

        writableEnv.put(key, newValue);
    }
}
