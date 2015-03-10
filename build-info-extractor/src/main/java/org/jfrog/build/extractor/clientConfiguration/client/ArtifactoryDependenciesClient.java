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
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.type.TypeReference;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Artifactory client to perform artifacts and build dependencies related tasks.
 *
 * @author Noam Y. Tenne
 */
public class ArtifactoryDependenciesClient {

    private String artifactoryUrl;

    private ArtifactoryHttpClient httpClient;

    public ArtifactoryDependenciesClient(String artifactoryUrl, String username, String password, Log logger) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password, logger);
    }

    public void setConnectionTimeout(int connectionTimeout) {
        httpClient.setConnectionTimeout(connectionTimeout);
    }

    public void setProxyConfiguration(String host, int port) {
        httpClient.setProxyConfiguration(host, port, null, null);
    }

    public void setProxyConfiguration(String host, int port, String username, String password) {
        httpClient.setProxyConfiguration(host, port, username, password);
    }

    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
    }


    /**
     * Retrieves list of {@link org.jfrog.build.api.dependency.BuildPatternArtifacts} for build dependencies specified.
     *
     * @param requests build dependencies to retrieve outputs for.
     * @return build outputs for dependencies specified.
     * @throws java.io.IOException
     */
    public List<BuildPatternArtifacts> retrievePatternArtifacts(List<BuildPatternArtifactsRequest> requests)
            throws IOException {
        final String json = new JsonSerializer<List<BuildPatternArtifactsRequest>>().toJSON(requests);
        final HttpPost post = new HttpPost(artifactoryUrl + "/api/build/patternArtifacts");

        StringEntity stringEntity = new StringEntity(json);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        post.setEntity(stringEntity);

        List<BuildPatternArtifacts> artifacts = readResponse(httpClient.getHttpClient().execute(post),
                new TypeReference<List<BuildPatternArtifacts>>() {
                },
                "Failed to retrieve build artifacts report");
        return artifacts;
    }


    public PatternResultFileSet searchArtifactsByPattern(String pattern) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String url = artifactoryUrl + "/api/search/pattern?pattern=" + pattern;
        PatternResultFileSet result = readResponse(client.execute(new HttpGet(url)),
                new TypeReference<PatternResultFileSet>() {
                },
                "Failed to search artifact by the pattern '" + pattern + "'");
        return result;
    }

    public PropertySearchResult searchArtifactsByProperties(String properties) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String replacedProperties = StringUtils.replaceEach(properties, new String[]{";", "+"}, new String[]{"&", ""});
        String url = artifactoryUrl + "/api/search/prop?" + replacedProperties;
        PropertySearchResult result = readResponse(client.execute(new HttpGet(url)),
                new TypeReference<PropertySearchResult>() {
                },
                "Failed to search artifact by the properties '" + properties + "'");
        return result;
    }


    /**
     * Reads HTTP response and converts it to object of the type specified.
     *
     * @param response     response to read
     * @param valueType    response object type
     * @param errorMessage error message to throw in case of error
     * @param <T>          response object type
     * @return response object converted from HTTP Json reponse to the type specified.
     * @throws java.io.IOException if reading or converting response fails.
     */
    private <T> T readResponse(HttpResponse response, TypeReference<T> valueType, String errorMessage)
            throws IOException {

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                return null;
            }

            InputStream content = null;

            try {
                content = entity.getContent();
                JsonParser parser = httpClient.createJsonParser(content);
                // http://wiki.fasterxml.com/JacksonDataBinding
                return parser.readValueAs(valueType);
            } finally {
                if (content != null) {
                    IOUtils.closeQuietly(content);
                }
            }
        } else {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                IOUtils.closeQuietly(httpEntity.getContent());
            }
            throw new IOException(errorMessage + ": " + response.getStatusLine());
        }
    }

    public HttpResponse downloadArtifact(String downloadUrl) throws IOException {
        return execute(downloadUrl, false);
    }

    public HttpResponse getArtifactChecksums(String artifactUrl) throws IOException {
        return execute(artifactUrl, true);
    }

    private HttpResponse execute(String artifactUrl, boolean isHead) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        artifactUrl = ArtifactoryHttpClient.encodeUrl(artifactUrl);
        HttpRequestBase httpRequest = isHead ? new HttpHead(artifactUrl) : new HttpGet(artifactUrl);

        //Explicitly force keep alive
        httpRequest.setHeader("Connection", "Keep-Alive");
        HttpResponse response = client.execute(httpRequest);
        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            EntityUtils.consume(response.getEntity());
            throw new FileNotFoundException("Unable to find " + artifactUrl);
        }

        if (statusCode != HttpStatus.SC_OK) {
            EntityUtils.consume(response.getEntity());
            throw new IOException("Error downloading " + artifactUrl + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
        return response;
    }
}