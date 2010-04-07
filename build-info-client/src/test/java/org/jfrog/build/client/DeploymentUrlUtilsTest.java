/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jfrog.build.client;

import org.jfrog.build.api.constants.BuildInfoProperties;
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
        props.put(BuildInfoProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + "buildNumber", "1");
        props.put(BuildInfoProperties.BUILD_INFO_DEPLOY_PROP_PREFIX + "buildName", "moo");
        String deploymentUrl = DeploymentUrlUtils.getDeploymentUrl(artifactoryUrl, props);
        Assert.assertEquals(deploymentUrl, artifactoryUrl + ";buildName=moo;buildNumber=1");
    }

}
