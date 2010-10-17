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

import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration test for {@link ArtifactoryBuildInfoClient}.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryBuildInfoClientTest {

    //private String artifactoryUrl = "http://localhost:8080/artifactory";
    private String artifactoryUrl = "http://localhost:8081/artifactory";

    public void getLocalRepositoriesKeys() throws IOException {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, new NullLog());
        List<String> repositoryKeys = client.getLocalRepositoriesKeys();
        assertNotNull(repositoryKeys, "Repositories keys should not be null");
        assertTrue(repositoryKeys.size() > 0, "Expected to get some repositories");
    }

    @Test(enabled = false, expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".* Unauthorized")
    public void postBuildInfoWithBadCredentials() throws IOException {
        Build build = new Build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl,
                "no-such-user", "test", new NullLog());

        client.sendBuildInfo(build);
    }

    public void postBuildInfo() throws IOException {
        Build build = new BuildInfoBuilder("build").startedDate(new Date()).number("123").build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password",
                new NullLog());
        client.sendBuildInfo(build);
    }

    @Test(enabled = false)
    public void deployFile() throws IOException {
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password", new NullLog());
        for (int i = 0; i < 10; i++) {
            String version = "1." + i;
            DeployDetails details = new DeployDetails.Builder().targetRepository("libs-releases-local")
                    .artifactPath(String.format("/test/test/%s/test-%s.pom", version, version))
                    .file(new File("build-info-client/pom.xml"))
                    .addProperty("key" + i, "value " + i)
                    .addProperty("key" + i + "b", "value " + i + "b").build();
            client.deployArtifact(details);
        }
    }

    /*public void uploadChecksums() throws IOException {
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password");
        File file = new File("build-info-client/pom.xml");
        client.uploadChecksums(file, artifactoryUrl);
    }*/
}
