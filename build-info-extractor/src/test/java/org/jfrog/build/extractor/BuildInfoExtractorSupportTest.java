/*
 * Copyright (C) 2010 JFrog Ltd.
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

/**
 * Test the build info extractor
 *
 * @author Tomer Cohen
 */
@Test
public class BuildInfoExtractorSupportTest {
    private static final String POPO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "popo";
    private static final String MOMO_KEY = BuildInfoProperties.BUILD_INFO_PROP_PREFIX + "momo";

    public void getBuildInfoPropertiesFromSystemProps() throws IOException {
        System.setProperty(POPO_KEY, "buildname");
        System.setProperty(MOMO_KEY, "1");

        Properties props = BuildInfoExtractorUtils.getBuildInfoProperties();

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

        Properties fileProps = BuildInfoExtractorUtils.getBuildInfoProperties();

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

        Properties buildInfoProperties = BuildInfoExtractorUtils.getBuildInfoProperties();
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
}