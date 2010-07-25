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

package org.jfrog.build.client;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Properties;

/**
 * @author Tomer Cohen
 */
@Test
public class DeploymentUrlUtilsTest {

    public void getDeploymentUrl() {
        String artifactoryUrl = "http://localhost:8080/artifactory/libs-releases-local";
        Properties props = System.getProperties();
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "buildName", "moo");
        props.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + "buildNumber", "1");
        String deploymentUrl = DeploymentUrlUtils.getDeploymentUrl(artifactoryUrl, props);
        Assert.assertEquals(deploymentUrl, artifactoryUrl + ";buildNumber=1;buildName=moo");
    }

}
