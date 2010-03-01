package org.artifactory.build.client;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.artifactory.build.api.Build;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
    private static final String BUILD_REST_RUL = "/api/build";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 120000;    // 2 Minutes

    private final String artifactoryUrl;
    private final String userName;
    private final String password;

    public ArtifactoryBuildInfoClient(String artifactoryUrl) {
        this(artifactoryUrl, null, null);
    }

    public ArtifactoryBuildInfoClient(String artifactoryUrl, String userName, String password) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
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

    public void sendBuildInfo(Build buildInfo) throws IOException {
        String url = artifactoryUrl + BUILD_REST_RUL;
        PreemptiveHttpClient client = createHttpClient(userName, password);
        HttpPut httpPut = new HttpPut(url);
        String buildInfoJson = buildInfoToJsonString(buildInfo);
        StringEntity stringEntity = new StringEntity(buildInfoJson);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        httpPut.setEntity(stringEntity);
        log.info("Deploying build info to: " + url);
        HttpResponse response = client.execute(httpPut);
        if (response.getEntity() != null) {
            response.getEntity().consumeContent();
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new IOException("Failed to send build info: " + response.getStatusLine().getReasonPhrase());
        }
    }

    private PreemptiveHttpClient createHttpClient(String userName, String password) {
        return new PreemptiveHttpClient(userName, password, DEFAULT_CONNECTION_TIMEOUT);
    }

    private JsonParser createJsonParser(InputStream in) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createJsonParser(in);
    }

    private JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    private String buildInfoToJsonString(Build buildInfo) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();

        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();

        jsonGenerator.writeObject(buildInfo);
        String result = writer.getBuffer().toString();
        return result;
    }
}
