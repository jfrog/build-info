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

package org.jfrog.build.extractor.client;

import com.google.common.collect.ArrayListMultimap;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * @author Tomer Cohen
 */
@Test
public class DeploymentUrlUtilsTest {

    public void getDeploymentUrl() throws UnsupportedEncodingException {
        String artifactoryUrl = "http://localhost:8080/artifactory/libs-releases-local";
        Properties props = new Properties(System.getProperties());
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "buildName", "moo");
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "buildNumber", "1");
        String deploymentUrl = DeploymentUrlUtils.getDeploymentUrl(artifactoryUrl, props);
        Assert.assertEquals(deploymentUrl, artifactoryUrl + ";buildName=moo;buildNumber=1");
    }

    public void getDeploymentUrlWithEncodingNeeded() throws UnsupportedEncodingException {
        String artifactoryUrl = "http://localhost:8080/artifactory/libs-releases-local";
        Properties props = new Properties(System.getProperties());
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "build Name", "moo");
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "build Number", "1");
        String deploymentUrl = DeploymentUrlUtils.getDeploymentUrl(artifactoryUrl, props);
        Assert.assertEquals(deploymentUrl, artifactoryUrl + ";build+Number=1;build+Name=moo");
    }


    public void testKeyWithMultiValuesParam() throws UnsupportedEncodingException {
        ArrayListMultimap<String, String> params = ArrayListMultimap.create();
        params.put("key", "valueA");
        params.put("key", "valueB");
        params.put("keyA", "valueA");

        String matrixParamString = DeploymentUrlUtils.buildMatrixParamsString(params, true);

        Assert.assertEquals(matrixParamString, ";keyA=valueA;key=valueA;key=valueB",
                "Unexpected matrix param with multi values: " + matrixParamString);
    }

    public void testEncodePath() throws UnsupportedEncodingException {
        Assert.assertEquals(DeploymentUrlUtils.encodePath("a/b/c"), "a/b/c");
        Assert.assertEquals(DeploymentUrlUtils.encodePath("a+/b+/c"), "a%2B/b%2B/c");
        Assert.assertEquals(DeploymentUrlUtils.encodePath("a /b /c"), "a%20/b%20/c");
        Assert.assertEquals(DeploymentUrlUtils.encodePath("a/b/c;a=1"), "a/b/c;a=1");
        Assert.assertEquals(DeploymentUrlUtils.encodePath("a/b/c;a=1;b=2"), "a/b/c;a=1;b=2");
    }

}
