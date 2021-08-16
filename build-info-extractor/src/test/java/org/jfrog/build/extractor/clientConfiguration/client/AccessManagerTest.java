package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.extractor.clientConfiguration.client.access.AccessManager;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Test
public class AccessManagerTest extends IntegrationTestsBase {
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
        String accessUrl = getPlatformUrl() + "/access";
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
        Assert.assertThrows(IOException.class, () -> accessManager.getProject(PROJECT_KEY));
    }

    @AfterClass
    protected void terminate() {
        try {
            accessManager.deleteProject(PROJECT_KEY);
        } catch (Exception ignored) {

        }
    }
}
