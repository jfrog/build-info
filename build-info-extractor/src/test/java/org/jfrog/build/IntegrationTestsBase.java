package org.jfrog.build;

import com.google.common.io.Closeables;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.fail;

/**
 * Prepares the infrastructure resources used by tests.
 * Created by diman on 27/02/2017.
 */
public abstract class IntegrationTestsBase {

    private String username;
    private String password;
    private String url;
    protected String localRepo = "build-info-tests-local";
    protected String remoteRepo;
    protected String virtualRepo = "build-info-tests-virtual";
    protected ArtifactoryBuildInfoClient buildInfoClient;
    protected ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    protected ArtifactoryDependenciesClient dependenciesClient;
    protected static final Log log = new TestingLog();

    private PreemptiveHttpClient preemptiveHttpClient;

    protected static final String LOCAL_REPO_PLACEHOLDER = "${LOCAL_REPO}";
    protected static final String VIRTUAL_REPO_PLACEHOLDER = "${VIRTUAL_REPO}";
    protected static final String TEMP_FOLDER_PLACEHOLDER = "${TEMP_FOLDER}";
    protected static final String UPLOAD_SPEC = "upload.json";
    protected static final String DOWNLOAD_SPEC = "download.json";
    protected static final String EXPECTED = "expected.json";

    private static final String BITESTS_ARTIFACTORY_ENV_VAR_PREFIX = "BITESTS_ARTIFACTORY_";
    private static final String BITESTS_ARTIFACTORY_PROPERTIES_PREFIX = "bitests.artifactory.";
    private static final String API_REPOSITORIES = "api/repositories";

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
        preemptiveHttpClient = createHttpClient().getHttpClient();
        buildInfoClient = createBuildInfoClient();
        buildInfoClientBuilder = createBuildInfoClientBuilder();
        dependenciesClient = createDependenciesClient();

        createTestRepo(localRepo);
        if (StringUtils.isNotBlank(remoteRepo)) {
            createTestRepo(remoteRepo);
        }
        createTestRepo(virtualRepo);
    }

    @AfterClass
    protected void terminate() throws IOException {
        // Delete the virtual first.
        deleteTestRepo(virtualRepo);
        if (StringUtils.isNotBlank(remoteRepo)) {
            deleteTestRepo(remoteRepo);
        }
        deleteTestRepo(localRepo);
        preemptiveHttpClient.close();
        buildInfoClient.close();
        dependenciesClient.close();
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
     * @param repo - repository name
     * @throws IOException
     */
    protected void deleteContentFromRepo(String repo) throws IOException {
        String fullItemUrl = url + repo + "/";
        String itemUrl = ArtifactoryHttpClient.encodeUrl(fullItemUrl);
        HttpRequestBase httpRequest = new HttpDelete(itemUrl);

        HttpResponse response = preemptiveHttpClient.execute(httpRequest);
        StatusLine statusLine = response.getStatusLine();
        EntityUtils.consumeQuietly(response.getEntity());
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new FileNotFoundException("Bad credentials for username: " + username);
        }

        if (!(200 <= statusCode && statusCode < 300)) {
            throw new IOException("Error deleting " + localRepo + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
    }

    /**
     * Check if repository exists.
     *
     * @param repo - repository name
     * @return
     * @throws IOException
     */
    private boolean isRepoExists(String repo) throws IOException {
        HttpRequestBase httpRequest = new HttpGet(getRepoApiUrl(repo));
        HttpResponse response = preemptiveHttpClient.execute(httpRequest);
        EntityUtils.consumeQuietly(response.getEntity());
        return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
    }

    /**
     * Create new repository according to the settings.
     *
     * @param repo - repository name
     * @throws IOException
     */
    private void createTestRepo(String repo) throws IOException {
        if (isRepoExists(repo)) {
            return;
        }
        HttpPut httpRequest = new HttpPut(getRepoApiUrl(repo));
        InputStream repoConfigInputStream = this.getClass().getResourceAsStream("/integration/settings/" + repo + ".json");
        InputStreamEntity repoConfigEntity = new InputStreamEntity(repoConfigInputStream, ContentType.create("application/json"));
        httpRequest.setEntity(repoConfigEntity);

        HttpResponse response = preemptiveHttpClient.execute(httpRequest);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
                throw new IOException("Error creating repository: " + repo + ". Code: " + statusCode + " Message: " +
                        response.getStatusLine().getReasonPhrase());
            }
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
            Closeables.closeQuietly(repoConfigInputStream);
        }
    }

    /**
     * Delete repository.
     *
     * @param repo - repository name
     * @throws IOException
     */
    private void deleteTestRepo(String repo) throws IOException {
        HttpRequestBase httpRequest = new HttpDelete(getRepoApiUrl(repo));
        HttpResponse response = preemptiveHttpClient.execute(httpRequest);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException("Error deleting repository" + repo + ". Code: " + statusCode + " Message: " +
                        response.getStatusLine().getReasonPhrase());
            }
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }
    }

    private String getRepoApiUrl(String repo) {
        String fullItemUrl = url + StringUtils.join(new String[]{API_REPOSITORIES, repo}, "/");
        return ArtifactoryHttpClient.encodeUrl(fullItemUrl);
    }

    /**
     * Read spec file and replace the placeholder test data.
     *
     * @param specFile
     * @return
     * @throws IOException
     */
    protected String readSpec(File specFile, String workSpacePath) throws IOException {
        String spec = FileUtils.readFileToString(specFile);
        spec = StringUtils.replace(spec, LOCAL_REPO_PLACEHOLDER, localRepo);
        spec = StringUtils.replace(spec, VIRTUAL_REPO_PLACEHOLDER, virtualRepo);
        spec = StringUtils.replace(spec, TEMP_FOLDER_PLACEHOLDER, workSpacePath);
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

    public static Log getLog() {
        return log;
    }

    private ArtifactoryBuildInfoClient createBuildInfoClient() {
        return new ArtifactoryBuildInfoClient(url, username, password, log);
    }

    private ArtifactoryBuildInfoClientBuilder createBuildInfoClientBuilder() {
        ArtifactoryBuildInfoClientBuilder builder = new ArtifactoryBuildInfoClientBuilder();
        return builder.setArtifactoryUrl(url).setUsername(username).setPassword(password).setLog(log);
    }

    private ArtifactoryDependenciesClient createDependenciesClient() {
        return new ArtifactoryDependenciesClient(url, username, password, log);
    }

    private ArtifactoryHttpClient createHttpClient() {
        return new ArtifactoryHttpClient(url, username, password, log);
    }

    /**
     * Expected inner class for testing proposes.
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