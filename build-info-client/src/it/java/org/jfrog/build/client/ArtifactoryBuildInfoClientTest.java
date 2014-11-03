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

package org.jfrog.build.client;

import com.thoughtworks.webstub.StubServerFacade;
import com.thoughtworks.webstub.dsl.HttpDsl;
import com.thoughtworks.webstub.dsl.builders.ResponseBuilder;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.webstub.StubServerFacade.newServer;
import static com.thoughtworks.webstub.dsl.builders.ResponseBuilder.response;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Integration test for {@link ArtifactoryBuildInfoClient}.
 *
 * @author Yossi Shaul
 */
@Test
public class ArtifactoryBuildInfoClientTest {

    //    private String artifactoryUrl = "http://localhost:8080/artifactory";
    private String artifactoryUrl = "http://localhost:8081/artifactory";

    private StubServerFacade server;
    private HttpDsl stubServer;

    @BeforeSuite
    public void beforeAll() {
        server = newServer(8081);
        stubServer = server.withContext("artifactory");
        server.start();
    }

    @BeforeTest
    public void beforeEach() {
        stubServer.reset();
        stubServer.get("/api/system/version").returns(response(200).withContent("{ \"version\": \"3.3.0\"}"));
    }

    @Test
    public void getLocalRepositoriesKeys() throws IOException {
        stubServer.get("/api/repositories?type=local").returns(response(200).withContent("[{" +
                "\"key\" : \"libs-releases-local\"," +
                "\"description\" : \"Local repository for in-house libraries\"," +
                "\"type\" : \"LOCAL\"," +
                "\"url\" : \"http://localhost:8081/artifactory/libs-releases-local\"" +
                "}]"));

        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl, new NullLog());

        List<String> repositoryKeys = client.getLocalRepositoriesKeys();
        assertTrue(repositoryKeys.contains("libs-releases-local"));
        assertEquals(repositoryKeys.size(), 1);
    }

    @Test(enabled = false, expectedExceptions = IOException.class, expectedExceptionsMessageRegExp = ".* Unauthorized")
    public void postBuildInfoWithBadCredentials() throws IOException {
        Build build = new Build();
        ArtifactoryBuildInfoClient client = new ArtifactoryBuildInfoClient(artifactoryUrl,
                "no-such-user", "test", new NullLog());

        client.sendBuildInfo(build);
    }

    @Test
    public void postBuildInfo() throws IOException {
        stubServer.put("/api/build").withHeader("Content-Type", "application/vnd.org.jfrog.artifactory+json").returns(response(204));

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

    @Test(enabled = false)
    public void deployFileWithChecksumHeader() throws IOException, NoSuchAlgorithmException {
        //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "debug");
        ArtifactoryBuildInfoClient client =
                new ArtifactoryBuildInfoClient(artifactoryUrl, "admin", "password", new NullLog());
        File testFile = new File(
                ArtifactoryBuildInfoClientTest.class.getResource("/org/jfrog/build/client/testfile.txt").getFile());

        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(testFile, "sha1");
        for (int i = 0; i < 10; i++) {
            String version = "1." + i;
            DeployDetails details = new DeployDetails.Builder().targetRepository("libs-releases-local")
                    .artifactPath(String.format("/test/test/%s/test-%s.gradle", version, version))
                    .file(testFile).sha1(checksums.get("sha1")).addProperty("key" + i, "value " + i)
                    .addProperty("key" + i + "b", "value " + i + "b").build();
            client.deployArtifact(details);
        }
    }

    @AfterSuite
    public void afterAll() {
        server.stop();
    }
}
