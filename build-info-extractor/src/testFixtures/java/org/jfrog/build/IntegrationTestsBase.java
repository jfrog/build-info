package org.jfrog.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.fail;

/**
 * Prepares the infrastructure resources used by tests.
 * Created by diman on 27/02/2017.
 */
public abstract class IntegrationTestsBase {

    protected static final Log log = new TestingLog();
    protected static final String LOCAL_REPO_PLACEHOLDER = "${LOCAL_REPO}";
    protected static final String LOCAL_REPO2_PLACEHOLDER = "${LOCAL_REPO2}";
    protected static final String VIRTUAL_REPO_PLACEHOLDER = "${VIRTUAL_REPO}";
    protected static final String TEMP_FOLDER_PLACEHOLDER = "${TEMP_FOLDER}";
    protected static final String LOCAL_REPOSITORIES_WILDCARD_PLACEHOLDER = "${LOCAL_REPO1_REPO2}";
    protected static final String UPLOAD_SPEC = "upload.json";
    protected static final String DOWNLOAD_SPEC = "download.json";
    protected static final String EXPECTED = "expected.json";
    protected static final String BITESTS_ARTIFACTORY_ENV_VAR_PREFIX = "BITESTS_ARTIFACTORY_";
    private static final String BITESTS_ARTIFACTORY_PROPERTIES_PREFIX = "bitests.artifactory.";
    protected String localRepo1 = "build-info-tests-local";
    protected String localRepo2 = "build-info-tests-local2";
    protected String remoteRepo;
    protected String virtualRepo = "build-info-tests-virtual";
    protected String localRepositoriesWildcard = "build-info-tests-local*";
    protected ArtifactoryManager artifactoryManager;
    protected ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private String username;
    private String password;
    private String url;

    public static Log getLog() {
        return log;
    }

    @BeforeClass
    public void init() throws IOException {
        Properties props = new Properties();
        // This file is not in GitHub. Create your own in src/test/resources or use environment variables.
        InputStream inputStream = this.getClass().getResourceAsStream("/artifactory-bi.properties");

        if (inputStream != null) {
            props.load(inputStream);
            inputStream.close();
        }

        url = readParam(props, "url");
        if (!url.endsWith("/")) {
            url += "/";
        }
        username = readParam(props, "username");
        password = readParam(props, "password");
        artifactoryManager = createArtifactoryManager();
        artifactoryManagerBuilder = createArtifactoryManagerBuilder();

        if (!artifactoryManager.getVersion().isOSS()) {
            if (StringUtils.isNotEmpty(localRepo1)) {
                createTestRepo(localRepo1);
            }
            if (StringUtils.isNotEmpty(remoteRepo)) {
                createTestRepo(remoteRepo);
            }
            if (StringUtils.isNotEmpty(virtualRepo)) {
                createTestRepo(virtualRepo);
            }
        }
    }

    @AfterClass
    protected void terminate() throws IOException {
        if (!artifactoryManager.getVersion().isOSS()) {
            // Delete the virtual first.
            if (StringUtils.isNotEmpty(virtualRepo)) {
                deleteTestRepo(virtualRepo);
            }
            if (StringUtils.isNotEmpty(remoteRepo)) {
                deleteTestRepo(remoteRepo);
            }
            if (StringUtils.isNotEmpty(localRepo1)) {
                deleteTestRepo(localRepo1);
            }
        }
        artifactoryManager.close();
    }

    private String readParam(Properties props, String paramName) {
        String paramValue = null;
        if (props.size() > 0) {
            paramValue = props.getProperty(BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + paramName);
        }
        if (paramValue == null) {
            paramValue = System.getProperty(BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + paramName);
        }
        if (paramValue == null) {
            paramValue = System.getenv(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + paramName.toUpperCase());
        }
        if (paramValue == null) {
            failInit();
        }
        return paramValue;
    }

    private void failInit() {
        String message =
                "Failed to load test Artifactory instance credentials. Looking for System properties:\n'" +
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "url', \n'" +
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "username' and \n'" +
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "password'. \n" +
                        "Or a properties file with those properties in classpath or Environment variables:\n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "URL', \n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "USERNAME' and \n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "PASSWORD'.";

        fail(message);
    }

    /**
     * Delete all content from the given repository.
     *
     * @param repo - repository name
     */
    protected void deleteContentFromRepo(String repo) throws IOException {
        if (!isRepoExists(repo)) {
            return;
        }
        artifactoryManager.deleteRepositoryContent(repo);
    }

    /**
     * Check if repository exists.
     *
     * @param repo - repository name
     */
    private boolean isRepoExists(String repo) throws IOException {
        return artifactoryManager.isRepositoryExist(repo);
    }

    /**
     * Create new repository according to the settings.
     *
     * @param repo - repository name
     */
    protected void createTestRepo(String repo) throws IOException {
        if (StringUtils.isBlank(repo) || isRepoExists(repo)) {
            return;
        }
        try (InputStream repoConfigInputStream = this.getClass().getResourceAsStream("/integration/settings/" + repo + ".json")) {
            String json = IOUtils.toString(repoConfigInputStream, StandardCharsets.UTF_8);
            artifactoryManager.createRepository(repo, json);
        }
    }

    /**
     * Delete repository.
     *
     * @param repo - repository name
     * @throws IOException
     */
    protected void deleteTestRepo(String repo) throws IOException {
        artifactoryManager.deleteRepository(repo);
    }

    /**
     * Read spec file and replace the placeholder test data.
     *
     */
    protected String readSpec(File specFile, String workSpacePath) throws IOException {
        String spec = FileUtils.readFileToString(specFile, StandardCharsets.UTF_8);
        spec = StringUtils.replace(spec, LOCAL_REPO_PLACEHOLDER, localRepo1);
        spec = StringUtils.replace(spec, LOCAL_REPO2_PLACEHOLDER, localRepo2);
        spec = StringUtils.replace(spec, VIRTUAL_REPO_PLACEHOLDER, virtualRepo);
        spec = StringUtils.replace(spec, TEMP_FOLDER_PLACEHOLDER, workSpacePath);
        spec = StringUtils.replace(spec, LOCAL_REPOSITORIES_WILDCARD_PLACEHOLDER, localRepositoriesWildcard);
        return StringUtils.replace(spec, "${WORKSPACE}", workSpacePath);
    }

    protected void verifyExpected(Expected expected, File workspace) {
        // Verify tempWorkspace exists
        Assert.assertTrue(workspace.exists(), "The path: '" + workspace.getPath() + "' does not exist");
        // Verify expected results
        Collection<File> downloadedFiles = FileUtils.listFiles(workspace, null, true);
        for (String path : expected.getFiles()) {
            File f = new File(workspace, path);
            Assert.assertTrue(downloadedFiles.contains(f), "Missing file: '" + path + "'.");
            downloadedFiles.remove(f);
        }

        for (File f : downloadedFiles) {
            Assert.fail("Unexpected file: '" + f.getPath() + "'.");
        }
    }

    protected String getUsername() {
        return this.username;
    }

    protected String getPassword() {
        return password;
    }

    protected String getUrl() {
        return this.url;
    }

    private ArtifactoryManager createArtifactoryManager() {
        return new ArtifactoryManager(url, username, password, log);
    }

    private ArtifactoryManagerBuilder createArtifactoryManagerBuilder() {
        ArtifactoryManagerBuilder builder = new ArtifactoryManagerBuilder();
        return builder.setArtifactoryUrl(url).setUsername(username).setPassword(password).setLog(log);
    }

    /**
     * Expected inner class for testing purposes.
     * Contains the local files expected to be found after successful download.
     */
    protected static class Expected {
        private List<String> files;

        public List<String> getFiles() {
            return files;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }
    }
}