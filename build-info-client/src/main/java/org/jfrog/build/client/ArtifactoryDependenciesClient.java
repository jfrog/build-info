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

package org.jfrog.build.client;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.type.TypeReference;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.util.JsonSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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


    public void downloadArtifact(String downloadUrl, File dest) throws IOException {
        HttpResponse response = executeGet(downloadUrl);

        if (dest.exists()) {
            dest.delete();
            dest.createNewFile();
        } else {
            dest.getParentFile().mkdirs();
            dest.createNewFile();
        }
        InputStream inputStream = response.getEntity().getContent();
        FileOutputStream fileOutputStream = new FileOutputStream(dest);
        try {
            IOUtils.copyLarge(inputStream, fileOutputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
            response.getEntity().consumeContent();
        }
    }

    public String downloadChecksum(String downloadUrl, String checksumAlgorithm) throws IOException {
        HttpResponse response = executeGet(downloadUrl + "." + checksumAlgorithm);

        InputStream inputStream = response.getEntity().getContent();
        try {
            return IOUtils.toString(inputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
            response.getEntity().consumeContent();
        }
    }

    private HttpResponse executeGet(String downloadUrl) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        HttpGet get = new HttpGet(downloadUrl);
        //Explicitly force keep alive
        get.setHeader("Connection", "Keep-Alive");

        HttpResponse response = client.execute(get);

        StatusLine statusLine = response.getStatusLine();
        int statusCode = statusLine.getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            throw new FileNotFoundException("Unable to find " + downloadUrl);
        }

        if (statusCode != HttpStatus.SC_OK) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            throw new IOException("Error downloading " + downloadUrl + ". Code: " + statusCode + " Message: " +
                    statusLine.getReasonPhrase());
        }
        return response;
    }
}