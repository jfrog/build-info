/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor;

import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.BuildInfoProperties;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

/**
 * Test the build info extractor
 *
 * @author Tomer Cohen
 */
@Test
public class BuildInfoExtractorSupportTest {
    private static final String POPO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "popo";
    private static final String MOMO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "momo";
    private static final String ENV_POPO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "popo";
    private static final String ENV_MOMO_KEY = BuildInfoProperties.BUILD_INFO_ENVIRONMENT_PREFIX + "momo";

    public void getBuildInfoPropertiesFromSystemProps() throws IOException {
        System.setProperty(POPO_KEY, "buildname");
        System.setProperty(MOMO_KEY, "1");

        Properties props = BuildInfoExtractorUtils.filterDynamicProperties(
                BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(new Properties()),
                BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);

        assertEquals(props.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(props.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(props.getProperty(MOMO_KEY), "1", "momo property does not match");
        System.clearProperty(POPO_KEY);
        System.clearProperty(MOMO_KEY);
    }

    public void getBuildInfoPropertiesFromFile() throws IOException {
        File propsFile = new File("tempPropFile");
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(POPO_KEY, "buildname");
        props.put(MOMO_KEY, "1");
        props.store(new FileOutputStream(propsFile), "");

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propsFile.getAbsolutePath());

        Properties fileProps = BuildInfoExtractorUtils.filterDynamicProperties(
                BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(new Properties()),
                BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);

        assertEquals(fileProps.size(), 2, "there should only be 2 properties after the filtering");
        assertEquals(fileProps.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(MOMO_KEY), "1", "momo property does not match");

        propsFile.delete();

        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
    }

    public void getBuildInfoProperties() throws IOException {
        // create a property file
        File propsFile = new File("tempPropFile");
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(POPO_KEY, "buildname");
        props.put(MOMO_KEY, "1");
        props.store(new FileOutputStream(propsFile), "");

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propsFile.getAbsolutePath());

        // Put system properties
        String kokoKey = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "koko";
        String gogoKey = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "2");

        Properties buildInfoProperties = BuildInfoExtractorUtils.filterDynamicProperties(
                BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(new Properties()),
                BuildInfoExtractorUtils.BUILD_INFO_PROP_PREDICATE);

        assertEquals(buildInfoProperties.size(), 4, "There should be 4 properties");
        assertEquals(buildInfoProperties.getProperty(POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty(kokoKey), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty(gogoKey), "2", "gogo parent number property does not match");

        propsFile.delete();
        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void getEnvPropertiesFromFile() throws IOException {
        File propsFile = new File("tempPropFile");
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(ENV_POPO_KEY, "buildname");
        props.put(ENV_MOMO_KEY, "1");
        props.store(new FileOutputStream(propsFile), "");

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propsFile.getAbsolutePath());

        Properties fileProps = BuildInfoExtractorUtils.getEnvProperties(new Properties(), null);
        assertEquals(fileProps.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(fileProps.getProperty(ENV_MOMO_KEY), "1", "momo property does not match");

        propsFile.delete();
        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
    }

    public void getEnvAndSysPropertiesFromFile() throws IOException {
        // create a property file
        File propsFile = new File("tempPropFile");
        propsFile.createNewFile();
        Properties props = new Properties();
        props.put(ENV_POPO_KEY, "buildname");
        props.put(ENV_MOMO_KEY, "1");
        props.store(new FileOutputStream(propsFile), "");

        System.setProperty(BuildInfoConfigProperties.PROP_PROPS_FILE, propsFile.getAbsolutePath());

        // Put system properties
        String kokoKey = "koko";
        String gogoKey = "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "2");

        Properties buildInfoProperties = BuildInfoExtractorUtils.getEnvProperties(new Properties(), null);
        assertEquals(buildInfoProperties.getProperty(ENV_POPO_KEY), "buildname", "popo property does not match");
        assertEquals(buildInfoProperties.getProperty(ENV_MOMO_KEY), "1", "momo number property does not match");
        assertEquals(buildInfoProperties.getProperty("koko"), "parent", "koko parent name property does not match");
        assertEquals(buildInfoProperties.getProperty("gogo"), "2", "gogo parent number property does not match");

        propsFile.delete();
        System.clearProperty(BuildInfoConfigProperties.PROP_PROPS_FILE);
        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void testExcludePatterns() throws IOException {
        // Put system properties
        String kokoKey = "koko";
        String koko2Key = "akoko";
        String gogoKey = "gogo";
        System.setProperty(kokoKey, "parent");
        System.setProperty(koko2Key, "parent2");
        System.setProperty(gogoKey, "2");

        Properties startProps = new Properties();
        startProps.put(BuildInfoConfigProperties.PROP_ENV_VARS_EXCLUDE_PATTERNS, "*koko");
        Properties buildInfoProperties = BuildInfoExtractorUtils.getEnvProperties(startProps, null);
        assertNull(buildInfoProperties.getProperty("koko"), "Should not find koko property due to exclude patterns");
        assertNull(buildInfoProperties.getProperty("akoko"), "Should not find akoko property due to exclude patterns");
        assertEquals(buildInfoProperties.getProperty("gogo"), "2", "gogo parent number property does not match");

        System.clearProperty(kokoKey);
        System.clearProperty(gogoKey);
    }

    public void testIncludePatterns() throws IOException {
        // Put system properties
        String gogoKey = "gogo1";
        String gogo2Key = "gogo2a";
        String kokoKey = "koko";
        System.setProperty(kokoKey, "parent");
        System.setProperty(gogoKey, "1");
        System.setProperty(gogo2Key, "2");

        Properties startProps = new Properties();
        startProps.put(BuildInfoConfigProperties.PROP_ENV_VARS_INCLUDE_PATTERNS, "gogo?*");
        Properties buildInfoProperties = BuildInfoExtractorUtils.getEnvProperties(startProps, null);
        assertEquals(buildInfoProperties.getProperty("gogo1"), "1", "gogo1 parent number property does not match");
        assertEquals(buildInfoProperties.getProperty("gogo2a"), "2", "gogo2a parent number property does not match");
        assertNull(buildInfoProperties.getProperty("koko"), "Should not find koko property due to include patterns");

        System.clearProperty(gogoKey);
        System.clearProperty(gogo2Key);
        System.clearProperty(kokoKey);
    }
}