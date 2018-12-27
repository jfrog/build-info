/*
 * Copyright (C) 2010 JFrog Ltd.
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

package org.jfrog.build.extractor.clientConfiguration.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * Artifactory client to perform artifacts and build dependencies related tasks.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryDependenciesClient extends ArtifactoryBaseClient {

    public ArtifactoryDependenciesClient(String artifactoryUrl, String username, String password, Log logger) {
        super(artifactoryUrl, username, password, logger);
    }

    /**
     * Retrieves list of {@link org.jfrog.build.api.dependency.BuildPatternArtifacts} for build dependencies specified.
     *
     * @param requests build dependencies to retrieve outputs for.
     * @return build outputs for dependencies specified.
     */
    public List<BuildPatternArtifacts> retrievePatternArtifacts(List<BuildPatternArtifactsRequest> requests)
            throws IOException {
        final String json = new JsonSerializer<List<BuildPatternArtifactsRequest>>().toJSON(requests);
        final HttpPost post = new HttpPost(artifactoryUrl + "/api/build/patternArtifacts");
        StringEntity stringEntity = new StringEntity(json);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        post.setEntity(stringEntity);
        InputStream responseStream = getResponseStream(httpClient.getHttpClient().execute(post), "Failed to retrieve build artifacts report");
        return readJsonResponse(responseStream,
                new TypeReference<List<BuildPatternArtifacts>>() {},
                false);
    }

    public PatternResultFileSet searchArtifactsByPattern(String pattern) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String url = artifactoryUrl + "/api/search/pattern?pattern=" + pattern;
        InputStream responseStream = getResponseStream(client.execute(new HttpGet(url)), "Failed to search artifact by the pattern '" + pattern + "'");
        return readJsonResponse(responseStream,
                new TypeReference<PatternResultFileSet>() {},
                false);
    }

    public PropertySearchResult searchArtifactsByProperties(String properties) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String replacedProperties = StringUtils.replaceEach(properties, new String[]{";", "+"}, new String[]{"&", ""});
        String url = artifactoryUrl + "/api/search/prop?" + replacedProperties;
        InputStream responseStream = getResponseStream(client.execute(new HttpGet(url)), "Failed to search artifact by the properties '" + properties + "'");
        return readJsonResponse(responseStream,
                new TypeReference<PropertySearchResult>() {},
                false);
    }

    public AqlSearchResult searchArtifactsByAql(String aql) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String url = artifactoryUrl + "/api/search/aql";
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(aql);
        httpPost.setEntity(entity);
        InputStream responseStream = getResponseStream(client.execute(httpPost), "Failed to search artifact by the aql '" + aql + "'");
        return readJsonResponse(responseStream,
                new TypeReference<AqlSearchResult>() {},
                true);
    }

    public Properties getNpmAuth() throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String url = artifactoryUrl + "/api/npm/auth";
        HttpGet httpGet = new HttpGet(url);
        InputStream responseStream = getResponseStream(client.execute(httpGet), "npm Auth request failed");
        return readPropertiesResponse(responseStream);
    }

    private InputStream getResponseStream(HttpResponse response, String errorMessage) throws IOException {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }
            return entity.getContent();
        } else {
            HttpEntity httpEntity = response.getEntity();
            EntityUtils.consume(httpEntity);
            throw new IOException(errorMessage + ": " + response.getStatusLine());
        }
    }

    /**
     * Reads HTTP response and converts it to object of the type specified.
     *
     * @param responseStream     response to read
     * @param valueType    response object type
     * @param <T>          response object type
     * @return response object converted from HTTP Json reponse to the type specified.
     * @throws java.io.IOException if reading or converting response fails.
     */
    private <T> T readJsonResponse(InputStream responseStream, TypeReference<T> valueType, boolean ignoreMissingFields)
            throws IOException {
        try {
            JsonParser parser = httpClient.createJsonParser(responseStream);
            if (ignoreMissingFields) {
                ((ObjectMapper) parser.getCodec()).configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
            }
            // http://wiki.fasterxml.com/JacksonDataBinding
            return parser.readValueAs(valueType);
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    private Properties readPropertiesResponse(InputStream responseStream) throws IOException {
        try {
            Properties properties = new Properties();
            properties.load(responseStream);
            return properties;
        } finally {
            IOUtils.closeQuietly(responseStream);
        }
    }

    public HttpResponse downloadArtifact(String downloadUrl) throws IOException {
        return execute(downloadUrl, false, null);
    }

    public HttpResponse downloadArtifact(String downloadUrl, Map<String, String> headers) throws IOException {
        return execute(downloadUrl, false, headers);
    }

    public HttpResponse getArtifactMetadata(String artifactUrl) throws IOException {
        return execute(artifactUrl, true, null);
    }

    public boolean isArtifactoryOSS() throws IOException {
        return !httpClient.getVersion().hasAddons();
    }

    private HttpResponse execute(String artifactUrl, boolean isHead, Map<String, String> headers) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        artifactUrl = ArtifactoryHttpClient.encodeUrl(artifactUrl);
        HttpRequestBase httpRequest = isHead ? new HttpHead(artifactUrl) : new HttpGet(artifactUrl);

        // Explicitly force keep alive
        httpRequest.setHeader("Connection", "Keep-Alive");
        // Add all required headers to the request
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                httpRequest.setHeader(entry.getKey(), entry.getValue());
            }
        }

        HttpResponse response = client.execute(httpRequest);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            throw new FileNotFoundException("Unable to find " + artifactUrl);
        }

        if (statusCode != HttpStatus.SC_OK && statusCode != HttpStatus.SC_PARTIAL_CONTENT) {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Error downloading " + artifactUrl + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
        return response;
    }
}