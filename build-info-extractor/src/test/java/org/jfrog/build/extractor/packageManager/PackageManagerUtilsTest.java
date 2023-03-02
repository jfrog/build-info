package org.jfrog.build.extractor.packageManager;

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

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getEnvProperties;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.filterBuildInfoProperties;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class PackageManagerUtilsTest {
    static String key1 = "test-env-key1";
    static String value1 = "test-env-value1";
    static String key2 = "test-env-key2";
    static String value2 = "test-env-value2";
    static String key3 = "test-env3";
    static String value3 = "test-env-key1";

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
        filter(buildInfo);

        // build info with properties
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "*" + key1);
        buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(props)
                .build();
        filter(buildInfo);

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
        filter(buildInfo);
    }

    @Test
    public void testExcludePatterns() {
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS, "*" + key1);

        BuildInfo buildInfo = createBuildInfoTest(props);
        filter(buildInfo);

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
    public void testIncludePatterns() {
        Properties props = new Properties();
        props.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "*" + key1);

        BuildInfo buildInfo = createBuildInfoTest(props);
        filter(buildInfo);

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

    private BuildInfo createBuildInfoTest(Properties props) {
        Properties buildInfoProperties = getEnvProperties(props, null);
        Properties moduleProps = new Properties();
        moduleProps.setProperty(key1, value1);
        moduleProps.setProperty("dummy-prefix" + key1, value1);
        moduleProps.setProperty(key2, value2);
        moduleProps.setProperty(key3, value3);
        final Module module = new Module();
        module.setId("foo");
        module.setProperties(moduleProps);
        BuildInfo buildInfo = new BuildInfoBuilder("BUILD_NAME")
                .number("BUILD_NUMBER")
                .startedDate(new Date())
                .properties(buildInfoProperties)
                .addModule(module).build();
        return buildInfo;
    }

    private void filter(BuildInfo buildInfo) {
        ArtifactoryClientConfiguration config = new ArtifactoryClientConfiguration(null);
        config.fillFromProperties(buildInfo.getProperties());
        filterBuildInfoProperties(config, buildInfo);
    }
}
