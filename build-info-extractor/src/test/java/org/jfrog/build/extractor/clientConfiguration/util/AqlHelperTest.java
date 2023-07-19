package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.filespecs.entities.FilesGroup;
import org.mockserver.integration.ClientAndServer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.AqlHelper.LAST_RELEASE;
import static org.jfrog.build.extractor.clientConfiguration.util.AqlHelper.LATEST;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testng.Assert.assertEquals;

public class AqlHelperTest {
    @DataProvider
    private Object[][] latestBuildNumberProvider() {
        return new Object[][]{
                {"123", "123", "123"},
                {"", "123", "123"},
                {LATEST, "123", "123"},
                {LAST_RELEASE, "123", "123"},
                {"", "", ""}
        };
    }

    @Test(dataProvider = "latestBuildNumberProvider")
    public void latestBuildNumberTest(String inputBuildNumber, String outputBuildNumber, String expectedBuildNumber) throws IOException {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8888);
             ArtifactoryManager mockArtifactoryManager = new ArtifactoryManager("http://127.0.0.1:8888/artifactory", "", new NullLog())) {
            mockServer.when(request().withPath("/artifactory/api/system/version")).respond(response().withStatusCode(HttpStatus.SC_OK).withBody("{\"version\":\"1.2.3\",\"addons\":[\"aaa\"]}"));
            mockServer.when(request().withPath("/artifactory/api/build/patternArtifacts")).respond(response().withStatusCode(HttpStatus.SC_OK).withBody(String.format("[{\"buildName\":\"indiana\",\"buildNumber\":\"%s\"}]", outputBuildNumber)));

            FilesGroup filesGroup = new FilesGroup();
            filesGroup.setBuild("indiana" + (StringUtils.isNotBlank(inputBuildNumber) ? "/" + inputBuildNumber : ""));
            AqlHelper aqlHelper = new AqlHelper(mockArtifactoryManager, new NullLog(), filesGroup);
            assertEquals(aqlHelper.buildNumber, expectedBuildNumber);
        }
    }
}
