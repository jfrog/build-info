package org.jfrog.build.extractor.packageManager;

import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoConfigProperties.*;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getEnvProperties;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.containsSuspectedSecrets;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.filterBuildInfoProperties;
import static org.testng.Assert.*;

public class PackageManagerUtilsTest {
    static String key1 = "test-env-key1";
    static String value1 = "test-env-value1";
    static String key2 = "test-env-key2";
    static String value2 = "test-env-value2";
    static String key3 = "test-env3";
    static String value3 = "AKCp8-test-env-key333333333est-env-key333333333est-env-key333333333est-env-key333333333";

    @BeforeClass
    public static void setup() {
        System.setProperty(key1, value1);
        System.setProperty(key2, value2);
        System.setProperty(key3, value3);
    }

    @AfterTest
    public static void tearDown() {
        System.clearProperty(key1);
        System.clearProperty(key2);
        System.clearProperty(key3);
    }

    @Test
    public void testEmptyExcludePatterns() {
        // build info with no properties
        BuildInfo buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(new Properties())
                .build();
        filterBuildInfoPropertiesTestHelper(buildInfo);

        // build info with properties
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "*" + key1);
        buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(props)
                .build();
        filterBuildInfoPropertiesTestHelper(buildInfo);

        // build info with properties in modules
        Properties moduleProps = new Properties();
        moduleProps.setProperty(key1, value1);
        final Module module = new Module();
        module.setId("foo");
        module.setProperties(moduleProps);
        buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(new Properties())
                .addModule(module).build();
        filterBuildInfoPropertiesTestHelper(buildInfo);
    }

    @Test
    public void testExcludePatterns() {
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS, "*" + key1);

        BuildInfo buildInfo = createBuildInfo(props);
        filterBuildInfoPropertiesTestHelper(buildInfo);

        // Excluded build info property by key
        assertNull(buildInfo.getProperties().getProperty(key1), "Should not find '" + key1 + "' property due to exclude patterns");
        // Not Excluded build info property by key
        assertEquals(buildInfo.getProperties().getProperty(key2), value2, key2 + " property does not match");
        // Excluded build info property by value
        assertNull(buildInfo.getProperties().getProperty(key3), "Should not find '" + key3 + "' property due to exclude patterns");

        // Excluded module property by key
        assertNull(buildInfo.getModule("foo").getProperties().getProperty(key1), "Should not find '" + key1 + "' property due to exclude patterns");
        // Excluded module property by key
        assertNull(buildInfo.getModule("foo").getProperties().getProperty("dummy-prefix" + key1), "Should not find 'dummy-prefix" + key1 + "' property due to exclude patterns");
        // Not excluded module property by key
        assertEquals(buildInfo.getModule("foo").getProperties().getProperty(key2), value2, key2 + " property does not match");
        // Excluded module property by value
        assertNull(buildInfo.getModule("foo").getProperties().getProperty(key3), "Should not find '" + key3 + "' property due to exclude patterns");

    }

    @Test
    public void testExcludeJfrogInternalKey() {
        Properties props = new Properties();
        Properties buildInfoProperties = getEnvProperties(props, new NullLog());
        Properties moduleProps = new Properties();
        moduleProps.setProperty(key1, value1);
        moduleProps.setProperty(ENV_PROPERTIES_FILE_KEY, value1);
        moduleProps.setProperty(ENV_PROPERTIES_FILE_KEY_IV, value1);
        moduleProps.setProperty(PROP_PROPS_FILE, value1);
        Module module = new Module();
        module.setId("foo");
        module.setProperties(moduleProps);
        BuildInfo buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .properties(buildInfoProperties)
                .startedDate(new Date())
                .properties(buildInfoProperties)
                .addModule(module).build();

        filterBuildInfoPropertiesTestHelper(buildInfo);

        // Excluded build info JFrog internal keys
        assertFalse(buildInfo.getProperties().containsKey(PROP_PROPS_FILE), "Should not find '" + PROP_PROPS_FILE + "' property due to exclude JFrog internal key");
        assertFalse(buildInfo.getProperties().containsKey(ENV_PROPERTIES_FILE_KEY_IV), "Should not find '" + ENV_PROPERTIES_FILE_KEY_IV + "' property due to exclude JFrog internal key");
        assertFalse(buildInfo.getProperties().containsKey(ENV_PROPERTIES_FILE_KEY), "Should not find '" + ENV_PROPERTIES_FILE_KEY + "' property due to exclude JFrog internal key");

        // Keep build info property
        assertEquals(buildInfo.getModule("foo").getProperties().getProperty(key1), value1, key1 + " property does not match");
        assertTrue(buildInfo.getProperties().containsKey(key1), "Should find '" + key1 + "'");
    }

    @Test
    public void testIncludePatterns() {
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "*" + key1);

        BuildInfo buildInfo = createBuildInfo(props);
        filterBuildInfoPropertiesTestHelper(buildInfo);

        // Included build info property by key
        assertEquals(buildInfo.getProperties().getProperty(key1), value1, key1 + " property does not match");
        assertNull(buildInfo.getProperties().getProperty(key2), "Should not find '" + key2 + "' property due to exclude patterns");
        assertNull(buildInfo.getProperties().getProperty(key3), "Should not find '" + key3 + "' property due to exclude patterns");

        // Included module property by key
        assertEquals(buildInfo.getModule("foo").getProperties().getProperty(key1), value1, key1 + " property does not match");
        assertEquals(buildInfo.getModule("foo").getProperties().getProperty("dummy-prefix" + key1), value1, key1 + " property does not match");
        assertNull(buildInfo.getModule("foo").getProperties().getProperty(key2), "Should not find " + key2 + " property due to include patterns");
        assertNull(buildInfo.getModule("foo").getProperties().getProperty(key3), "Should not find " + key3 + " property due to include patterns");
    }

    @Test
    public void testContainsSuspectedSecrets() {
        assertFalse(containsSuspectedSecrets(null));
        assertFalse(containsSuspectedSecrets(""));
        assertFalse(containsSuspectedSecrets("text"));
        assertFalse(containsSuspectedSecrets("text with space"));
        assertFalse(containsSuspectedSecrets("text with Capital"));
        assertFalse(containsSuspectedSecrets(" spacewith spacewith spacewith AKCp8with spacewith spacewith spacewith space"));

        assertTrue(containsSuspectedSecrets("AKCp8with spacewith spacewith spacewith spacewith spacewith spacewith space"));
        assertTrue(containsSuspectedSecrets("cmVmdGtuOjAxOjtext with pacewith spacewith spacewith spacewith spacewith space"));
        assertTrue(containsSuspectedSecrets("eyJ2ZXIiOiIyIiwidHlwIjoiSldUIiwiYWxnIjoiUlMyNTYiLCJraWQiOiJtext with Capital"));
    }

    private BuildInfo createBuildInfo(Properties props) {
        Properties buildInfoProperties = getEnvProperties(props, new NullLog());
        Properties moduleProps = new Properties();
        moduleProps.setProperty(key1, value1);
        moduleProps.setProperty("dummy-prefix" + key1, value1);
        moduleProps.setProperty(key2, value2);
        moduleProps.setProperty(key3, value3);
        Module module = new Module();
        module.setId("foo");
        module.setProperties(moduleProps);
        return new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(buildInfoProperties)
                .addModule(module).build();
    }

    private void filterBuildInfoPropertiesTestHelper(BuildInfo buildInfo) {
        ArtifactoryClientConfiguration config = new ArtifactoryClientConfiguration(null);
        config.fillFromProperties(buildInfo.getProperties());
        filterBuildInfoProperties(config, buildInfo, new NullLog());
    }
}
