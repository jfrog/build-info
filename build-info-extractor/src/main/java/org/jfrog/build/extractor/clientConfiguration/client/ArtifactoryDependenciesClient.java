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
import org.jfrog.build.api.dependency.*;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.PreemptiveHttpClient;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
                "Failed to retrieve build artifacts report",
                false);
        return artifacts;
    }

    public PatternResultFileSet searchArtifactsByPattern(String pattern) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String url = artifactoryUrl + "/api/search/pattern?pattern=" + pattern;
        PatternResultFileSet result = readResponse(client.execute(new HttpGet(url)),
                new TypeReference<PatternResultFileSet>() {
                },
                "Failed to search artifact by the pattern '" + pattern + "'",
                false);
        return result;
    }

    public PropertySearchResult searchArtifactsByProperties(String properties) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();
        String replacedProperties = StringUtils.replaceEach(properties, new String[]{";", "+"}, new String[]{"&", ""});
        String url = artifactoryUrl + "/api/search/prop?" + replacedProperties;
        PropertySearchResult result = readResponse(client.execute(new HttpGet(url)),
                new TypeReference<PropertySearchResult>() {
                },
                "Failed to search artifact by the properties '" + properties + "'",
                false);
        return result;
    }

    public AqlSearchResult searchArtifactsByAql(String aql) throws IOException {
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String url = artifactoryUrl + "/api/search/aql";
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(aql);
        httpPost.setEntity(entity);
        AqlSearchResult result = readResponse(client.execute(httpPost),
                new TypeReference<AqlSearchResult>() {
                },
                "Failed to search artifact by the aql '" + aql + "'",
                true);
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
    private <T> T readResponse(HttpResponse response, TypeReference<T> valueType, String errorMessage, boolean ignorMissingFields)
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
                if (ignorMissingFields) {
                    ((ObjectMapper) parser.getCodec()).configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                }
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

    public boolean isArtifactoryOSS() throws IOException {
        return !httpClient.getVersion().hasAddons();
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