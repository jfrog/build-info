package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.client.access.AccessManager;
import org.jfrog.build.extractor.clientConfiguration.client.response.CreateAccessTokenResponse;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.LogEventRequestAndResponse;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.BROWSER_LOGIN_ENDPOINT;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.testng.Assert.assertEquals;

@Test
public class AccessManagerTest extends IntegrationTestsBase {
    private static final String GET_BROWSER_LOGIN_TOKEN_RESPONSE = "{\"token_id\": \"token_id_val\",\"access_token\": \"access_token_val\"," +
            "\"refresh_token\": \"refresh_token_val\",\"expires_in\": 10000,\"scope\": \"applied-permissions/user\",\"token_type\": \"Bearer\"}";

    // Project key has maximum size of 6 chars, and must start with a lowercase letter.
    private static final String PROJECT_KEY = "bi" + StringUtils.right(String.valueOf(System.currentTimeMillis()), 4);
    protected static final String PROJECT_KEY_PLACEHOLDER = "${PROJECT_KEY}";
    protected static final String COMMAND_TYPE_PLACEHOLDER = "${COMMAND_TYPE}";
    protected static final String COMMAND_TYPE_CREATED = "CREATED";
    protected static final String COMMAND_TYPE_UPDATED = "UPDATED";
    protected static final String PROJECT_CONF = "conf.json";
    private static final String PROJECTS_TEST_PATH = "/projectsConf";

    private AccessManager accessManager;

    @BeforeClass
    @Override
    public void init() throws IOException {
        super.init();
        String accessUrl = getPlatformUrl() + "access";
        accessManager = new AccessManager(accessUrl, getAdminToken(), getLog());
    }

    @Test
    public void projectTest() throws IOException, URISyntaxException {
        assertProjectDoesntExist();
        String curProjectConf = readProjectConfiguration(COMMAND_TYPE_CREATED);
        accessManager.createProject(curProjectConf);
        assertProjectExists(COMMAND_TYPE_CREATED);
        curProjectConf = readProjectConfiguration(COMMAND_TYPE_UPDATED);
        accessManager.updateProject(PROJECT_KEY, curProjectConf);
        assertProjectExists(COMMAND_TYPE_UPDATED);
        accessManager.deleteProject(PROJECT_KEY);
        assertProjectDoesntExist();
    }

    public void sendBrowserLoginRequestTest() throws IOException {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8888);
             AccessManager mockAccessManager = new AccessManager("http://127.0.0.1:8888/access", "", getLog())
        ) {
            UUID uuid = UUID.randomUUID();
            mockServer.when(request().withPath(String.format("/access/%s/request", BROWSER_LOGIN_ENDPOINT))).respond(response().withStatusCode(HttpStatus.SC_OK));
            mockAccessManager.sendBrowserLoginRequest(uuid.toString());
            LogEventRequestAndResponse[] events = mockServer.retrieveRecordedRequestsAndResponses(request().withPath(String.format("/access/%s/request", BROWSER_LOGIN_ENDPOINT)));
            assertEquals(1, events.length);
            assertEquals(HttpStatus.SC_OK, events[0].getHttpResponse().getStatusCode().intValue());
        }
    }

    public void getBrowserLoginRequestTokenTest() throws IOException {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8888);
             AccessManager mockAccessManager = new AccessManager("http://127.0.0.1:8888/access", "", getLog())
        ) {
            UUID uuid = UUID.randomUUID();
            mockServer.when(request().withPath(String.format("/access/%s/token/%s", BROWSER_LOGIN_ENDPOINT, uuid))).respond(response().withBody(GET_BROWSER_LOGIN_TOKEN_RESPONSE));
            CreateAccessTokenResponse createAccessTokenResponse = mockAccessManager.getBrowserLoginRequestToken(uuid.toString());
            Assert.assertNotNull(createAccessTokenResponse);
            assertEquals(createAccessTokenResponse.getTokenId(), "token_id_val");
            assertEquals(createAccessTokenResponse.getAccessToken(), "access_token_val");
            assertEquals(createAccessTokenResponse.getRefreshToken(), "refresh_token_val");
            assertEquals(createAccessTokenResponse.getExpiresIn(), 10000);
            assertEquals(createAccessTokenResponse.getScope(), "applied-permissions/user");
            assertEquals(createAccessTokenResponse.getTokenType(), "Bearer");
        }
    }

    private String readProjectConfiguration(String commandType) throws IOException, URISyntaxException {
        File projectsTestPath = new File(this.getClass().getResource(PROJECTS_TEST_PATH).toURI()).getCanonicalFile();
        File confFile = new File(projectsTestPath, PROJECT_CONF);
        String spec = FileUtils.readFileToString(confFile, StandardCharsets.UTF_8);
        spec = StringUtils.replace(spec, PROJECT_KEY_PLACEHOLDER, PROJECT_KEY);
        return StringUtils.replace(spec, COMMAND_TYPE_PLACEHOLDER, commandType);
    }

    private void assertProjectExists(String shouldContain) throws IOException {
        String result = accessManager.getProject(PROJECT_KEY);
        Assert.assertTrue(result.contains(shouldContain));
    }

    private void assertProjectDoesntExist() {
        Log oldLog = accessManager.log;
        accessManager.setLog(new NullLog());
        Assert.assertThrows(IOException.class, () -> accessManager.getProject(PROJECT_KEY));
        accessManager.setLog(oldLog);
    }

    @AfterClass
    protected void terminate() {
        try {
            accessManager.deleteProject(PROJECT_KEY);
            accessManager.close();
        } catch (Exception ignored) {

        }
    }
}
