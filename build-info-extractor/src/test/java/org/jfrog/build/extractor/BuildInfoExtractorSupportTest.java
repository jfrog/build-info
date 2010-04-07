package org.jfrog.build.extractor;

import org.jfrog.build.api.constants.BuildInfoProperties;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.testng.Assert.assertEquals;

/**
 * Test the build info extractor
 *
 * @author Tomer Cohen
 */
@Test
public class BuildInfoExtractorSupportTest {

    public void getBuildInfoPropertiesFromSystemProps() throws IOException {
        System.setProperty(BuildInfoProperties.PROP_BUILD_NAME, "buildname");
        System.setProperty(BuildInfoProperties.PROP_BUILD_NUMBER, "1");

        Properties props = BuildInfoExtractorSupport.getBuildInfoProperties();

        assertEquals(props.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(props.getProperty(BuildInfoProperties.PROP_BUILD_NAME), "buildname",
                "build name property is does not match");
        assertEquals(props.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER), "1",
                "build number property is does not match");
        System.clearProperty(BuildInfoProperties.PROP_BUILD_NAME);
        System.clearProperty(BuildInfoProperties.PROP_BUILD_NUMBER);
    }

    public void getBuildInfoPropertiesFromFile() throws IOException {
        File propsFile = new File(BuildInfoProperties.PROP_PROPS_FILE);
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(BuildInfoProperties.PROP_BUILD_NAME, "buildname");
        props.put(BuildInfoProperties.PROP_BUILD_NUMBER, "1");
        props.store(new FileOutputStream(propsFile), "");

        Properties fileProps = BuildInfoExtractorSupport.getBuildInfoProperties();

        assertEquals(fileProps.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(BuildInfoProperties.PROP_BUILD_NAME), "buildname",
                "build name property is does not match");
        assertEquals(fileProps.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER), "1",
                "build number property is does not match");

        propsFile.delete();
    }

    public void getBuildInfoProperties() throws IOException {

        // create a property file
        File propsFile = new File(BuildInfoProperties.PROP_PROPS_FILE);
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(BuildInfoProperties.PROP_BUILD_NAME, "buildname");
        props.put(BuildInfoProperties.PROP_BUILD_NUMBER, "1");
        props.store(new FileOutputStream(propsFile), "");

        // Put system properties
        System.setProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME, "parent");
        System.setProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER, "2");

        Properties buildInfoProperties = BuildInfoExtractorSupport.getBuildInfoProperties();
        assertEquals(buildInfoProperties.size(), 4, "There should be 4 properties");
        assertEquals(buildInfoProperties.getProperty(BuildInfoProperties.PROP_BUILD_NAME), "buildname",
                "build name property is does not match");
        assertEquals(buildInfoProperties.getProperty(BuildInfoProperties.PROP_BUILD_NUMBER), "1",
                "build number property is does not match");
        assertEquals(buildInfoProperties.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME), "parent",
                "build parent name property is does not match");
        assertEquals(buildInfoProperties.getProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER), "2",
                "build parent number property is does not match");

        propsFile.delete();
        System.clearProperty(BuildInfoProperties.PROP_PARENT_BUILD_NAME);
        System.clearProperty(BuildInfoProperties.PROP_PARENT_BUILD_NUMBER);
    }
}
