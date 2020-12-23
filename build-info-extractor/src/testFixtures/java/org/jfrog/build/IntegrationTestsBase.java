package org.jfrog.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
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
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
    protected String localRepo1 = "build-info-tests-local";
    protected String localRepo2 = "build-info-tests-local2";
    protected String remoteRepo;
    protected String virtualRepo = "build-info-tests-virtual";
    protected String localRepositoriesWildcard = "build-info-tests-local*";
    protected ArtifactoryBuildInfoClient buildInfoClient;
    protected ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    protected ArtifactoryDependenciesClient dependenciesClient;
    protected static final Log log = new TestingLog();

    private PreemptiveHttpClient preemptiveHttpClient;

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

        url = Optional.ofNullable(readParam(props, "url")).orElse("");
        if (!url.endsWith("/")) {
            url += "/";
        }
        username = readParam(props, "username");
        password = readParam(props, "password");
        preemptiveHttpClient = createHttpClient().getHttpClient();
        buildInfoClient = createBuildInfoClient();
        buildInfoClientBuilder = createBuildInfoClientBuilder();
        dependenciesClient = createDependenciesClient();

        if (!dependenciesClient.isArtifactoryOSS()) {
            createTestRepo(localRepo1);
            createTestRepo(remoteRepo);
            createTestRepo(virtualRepo);
        }
    }

    @AfterClass
    protected void terminate() throws IOException {
        if (!dependenciesClient.isArtifactoryOSS()) {
            // Delete the virtual first.
            deleteTestRepo(virtualRepo);
            deleteTestRepo(remoteRepo);
            deleteTestRepo(localRepo1);
        }
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
     *
     * @param repo - repository name
     * @throws IOException
     */
    protected void deleteContentFromRepo(String repo) throws IOException {
        if (!isRepoExists(repo)) {
            return;
        }
        String fullItemUrl = url + repo + "/";
        String itemUrl = ArtifactoryHttpClient.encodeUrl(fullItemUrl);
        HttpRequestBase httpRequest = new HttpDelete(itemUrl);

        try (CloseableHttpResponse response = preemptiveHttpClient.execute(httpRequest)) {
            EntityUtils.consumeQuietly(response.getEntity());
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
                throw new FileNotFoundException("Bad credentials for username: " + username);
            }
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException(getRepositoryCustomErrorMessage(repo, false, statusLine));
            }
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
        try (CloseableHttpResponse response = preemptiveHttpClient.execute(httpRequest)) {
            EntityUtils.consumeQuietly(response.getEntity());
            return response.getStatusLine().getStatusCode() == HttpStatus.SC_OK;
        }
    }

    /**
     * Create new repository according to the settings.
     *
     * @param repo - repository name
     * @throws IOException
     */
    protected void createTestRepo(String repo) throws IOException {
        if (StringUtils.isBlank(repo) || isRepoExists(repo)) {
            return;
        }
        HttpPut httpRequest = new HttpPut(getRepoApiUrl(repo));
        try (InputStream repoConfigInputStream = this.getClass().getResourceAsStream("/integration/settings/" + repo + ".json")) {
            InputStreamEntity repoConfigEntity = new InputStreamEntity(repoConfigInputStream, ContentType.create("application/json"));
            httpRequest.setEntity(repoConfigEntity);
            try (CloseableHttpResponse response = preemptiveHttpClient.execute(httpRequest)) {
                EntityUtils.consumeQuietly(response.getEntity());
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_CREATED) {
                    throw new IOException(getRepositoryCustomErrorMessage(repo, true, response.getStatusLine()));
                }
            }
        }
    }

    /**
     * Delete repository.
     *
     * @param repo - repository name
     * @throws IOException
     */
    protected void deleteTestRepo(String repo) throws IOException {
        if (StringUtils.isBlank(repo)) {
            return;
        }
        HttpRequestBase httpRequest = new HttpDelete(getRepoApiUrl(repo));
        try (CloseableHttpResponse response = preemptiveHttpClient.execute(httpRequest)) {
            EntityUtils.consumeQuietly(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new IOException(getRepositoryCustomErrorMessage(repo, false, response.getStatusLine()));
            }
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
        return new ArtifactoryDependenciesClient(url, username, password, StringUtils.EMPTY, log);
    }

    private ArtifactoryHttpClient createHttpClient() {
        return new ArtifactoryHttpClient(url, username, password, log);
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

    /***
     * Returns a custom exception error to be thrown when repository creation/deletion fails.
     * @param repo - repo name.
     * @param creating - creation or deletion failed.
     * @param statusLine - status returned.
     * @return - custom error message.
     */
    private String getRepositoryCustomErrorMessage(String repo, boolean creating, StatusLine statusLine) {
        StringBuilder builder = new StringBuilder()
                .append("Error ")
                .append(creating ? "creating" : "deleting")
                .append(" '").append(repo).append("'. ")
                .append("Code: ").append(statusLine.getStatusCode());
        if (statusLine.getReasonPhrase() != null) {
            builder.append(" Message: ")
                    .append(statusLine.getReasonPhrase());
        }
        return builder.toString();
    }
}