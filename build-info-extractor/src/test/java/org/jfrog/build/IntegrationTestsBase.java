package org.jfrog.build;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.testng.Assert.fail;

/**
 * Created by diman on 27/02/2017.
 */
public abstract class IntegrationTestsBase {

    private String username;
    private String password;
    private String url;
    protected String repo;
    protected NullLog log = new NullLog();
    private PreemptiveHttpClient client;

    protected static final String BITESTS_ARTIFACTORY_REPOSITORY_PLACEHOLDER = "${REPO}";
    protected static final String BITESTS_ARTIFACTORY_TEMP_FOLDER_PLACEHOLDER = "${TEMP_FOLDER}";
    private static final String BITESTS_ARTIFACTORY_ENV_VAR_PREFIX = "BITESTS_ARTIFACTORY_";
    private static final String BITESTS_ARTIFACTORY_PROPERTIES_PREFIX = "bitests.artifactory.";

    @BeforeTest
    public void init() throws IOException {
        Properties props = new Properties();
        // This file is not in GitHub. Create your own in src/test/resources or use environment variables.
        InputStream inputStream = this.getClass().getResourceAsStream("/artifactory-bi.properties");

        if (inputStream != null) {
            props.load(inputStream);
        }

        url = readParam(props, "url");
        if (!url.endsWith("/")) {
            url += "/";
        }
        username = readParam(props, "username");
        password = readParam(props, "password");
        repo = readParam(props, "repo");
        client = createHttpClient().getHttpClient();
    }

    @AfterTest
    protected void closePreemptiveHttpClient() {
        client.close();
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
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "repo', \n'" +
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "username' and \n'" +
                        BITESTS_ARTIFACTORY_PROPERTIES_PREFIX + "password'. \n" +
                        "Or a properties file with those properties in classpath or Environment variables:\n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "URL', \n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "REPO', \n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "USERNAME' and \n'" +
                        BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "PASSWORD'.";

        fail(message);
    }

    /**
     * The method deletes the provided in the url item (folder or artifact).
     * It uses the credentials, Artifactory url and repo that was provided by the environment.
     * for example, if the provided itemUrl was bar/foo.jar and the url was http://art1/art, the method will delete
     * http://art1/art/bar/foo.jar.
     * This method is created for Artifactory cleanup after successful run.
     *
     * @param itemUrl the inner url, without artifactory url and repo
     * @return the response from artifactory
     * @throws IOException in case of IO exception
     */
    protected HttpResponse deleteItemFromArtifactory(String itemUrl) throws IOException {
        String fullItemUrl = url + repo + "/" + itemUrl;
        itemUrl = ArtifactoryHttpClient.encodeUrl(fullItemUrl);
        HttpRequestBase httpRequest = new HttpDelete(itemUrl);

        //Explicitly force keep alive
        httpRequest.setHeader("Connection", "Keep-Alive");
        HttpResponse response = client.execute(httpRequest);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            EntityUtils.consume(response.getEntity());
            throw new FileNotFoundException("Bad credentials for username: " + username);
        }

        if (!(200 <= statusCode && statusCode < 300)) {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Error Deleting " + itemUrl + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
        return response;
    }

    protected ArtifactoryBuildInfoClient createBuildInfoClient() {
        return new ArtifactoryBuildInfoClient(url, username, password, log);
    }

    protected ArtifactoryDependenciesClient createDependenciesClient() {
        return new ArtifactoryDependenciesClient(url, username, password, log);
    }

    private ArtifactoryHttpClient createHttpClient() {
        return new ArtifactoryHttpClient(url, username, password, log);
    }
}