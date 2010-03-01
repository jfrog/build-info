package org.artifactory.build.client;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Artifactory client to perform build info related tasks.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryBuildInfoClient {
    private static final Log log = LogFactory.getLog(ArtifactoryBuildInfoClient.class);

    private static final String LOCAL_REPOS_REST_RUL = "/api/repositories?type=local";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 120000;    // 2 Minutes

    private String artifactoryUrl;
    private String userName;
    private String password;

    public ArtifactoryBuildInfoClient(String artifactoryUrl) {
        this(artifactoryUrl, null, null);
    }

    public ArtifactoryBuildInfoClient(String artifactoryUrl, String userName, String password) {
        this.artifactoryUrl = artifactoryUrl;
        this.userName = userName;
        this.password = password;
    }

    public List<String> getLocalRepositoriesKeys() {
        List<String> repositories = new ArrayList<String>();
        try {
            PreemptiveHttpClient client = createHttpClient(userName, password);

            String localReposUrl = artifactoryUrl + LOCAL_REPOS_REST_RUL;
            log.debug("Requesting local repositories list from: " + localReposUrl);
            HttpGet httpget = new HttpGet(localReposUrl);
            HttpResponse response = client.execute(httpget);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                log.warn("Failed to obtain list of repositories: " + response.getStatusLine());
                repositories = Collections.emptyList();
            } else {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    repositories = new ArrayList<String>();
                    JsonParser parser = createJsonParser(entity.getContent());
                    JsonNode result = parser.readValueAsTree();
                    log.debug("Repositories result = " + result);
                    for (JsonNode jsonNode : result) {
                        String repositoryKey = jsonNode.get("key").getTextValue();
                        repositories.add(repositoryKey);
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to obtain list of repositories: " + e.getMessage());
        }

        return repositories;
    }

    public PreemptiveHttpClient createHttpClient(String userName, String password) {
        return new PreemptiveHttpClient(userName, password, DEFAULT_CONNECTION_TIMEOUT);
    }

    private JsonParser createJsonParser(InputStream in) throws IOException {
        JsonFactory jsonFactory = new JsonFactory();
        JsonParser parser = jsonFactory.createJsonParser(in);
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        parser.setCodec(mapper);
        return parser;
    }
}
