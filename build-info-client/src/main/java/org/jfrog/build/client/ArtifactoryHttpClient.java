/*
 * Copyright (C) 2011 JFrog Ltd.
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

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.util.URI;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryHttpClient {

    private final Log log;

    public static final ArtifactoryVersion UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("2.2.3");
    public static final ArtifactoryVersion NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("2.2.4");
    public static final ArtifactoryVersion MINIMAL_ARTIFACTORY_VERSION = new ArtifactoryVersion("2.2.3");
    public static final String VERSION_INFO_URL = "/api/system/version";
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;    // 5 Minutes in seconds

    private final String artifactoryUrl;
    private final String username;
    private final String password;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
    private ProxyConfiguration proxyConfiguration;

    private PreemptiveHttpClient deployClient;

    public ArtifactoryHttpClient(String artifactoryUrl, String username, String password, Log log) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        this.username = username;
        this.password = password;
        this.log = log;
    }

    /**
     * Sets the proxy host and port.
     *
     * @param host Proxy host
     * @param port Proxy port
     */
    public void setProxyConfiguration(String host, int port) {
        setProxyConfiguration(host, port, null, null);
    }

    /**
     * Sets the proxy details.
     *
     * @param host     Proxy host
     * @param port     Proxy port
     * @param username Username to authenticate with the proxy
     * @param password Password to authenticate with the proxy
     */
    public void setProxyConfiguration(String host, int port, String username, String password) {
        proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.host = host;
        proxyConfiguration.port = port;
        proxyConfiguration.username = username;
        proxyConfiguration.password = password;
    }

    /**
     * Network timeout in seconds to use both for connection establishment and for unanswered requests.
     *
     * @param connectionTimeout Timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Release all connection and cleanup resources.
     */
    public void shutdown() {
        if (deployClient != null) {
            deployClient.shutdown();
        }
    }

    public PreemptiveHttpClient getHttpClient() {
        if (deployClient == null) {
            PreemptiveHttpClient client = new PreemptiveHttpClient(username, password, connectionTimeout);
            if (proxyConfiguration != null) {
                client.setProxyConfiguration(proxyConfiguration.host, proxyConfiguration.port,
                        proxyConfiguration.username, proxyConfiguration.password);
            }
            deployClient = client;
        }

        return deployClient;
    }

    public ArtifactoryVersion getVersion() throws IOException {
        String versionUrl = artifactoryUrl + VERSION_INFO_URL;
        PreemptiveHttpClient client = getHttpClient();
        HttpGet httpGet = new HttpGet(versionUrl);
        HttpResponse response = client.execute(httpGet);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            HttpEntity httpEntity = response.getEntity();
            if (httpEntity != null) {
                httpEntity.consumeContent();
            }
            return ArtifactoryVersion.NOT_FOUND;
        }
        if (statusCode != HttpStatus.SC_OK) {
            if (response.getEntity() != null) {
                response.getEntity().consumeContent();
            }
            throw new IOException(response.getStatusLine().getReasonPhrase());
        }
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            InputStream content = httpEntity.getContent();
            JsonParser parser;
            try {
                parser = createJsonParser(content);
                httpEntity.consumeContent();
                JsonNode result = parser.readValueAsTree();
                log.debug("Version result: " + result);
                String version = result.get("version").getTextValue();
                JsonNode addonsNode = result.get("addons");
                boolean hasAddons = (addonsNode != null) && addonsNode.iterator().hasNext();
                return new ArtifactoryVersion(version, hasAddons);
            } finally {
                if (content != null) {
                    content.close();
                }
            }
        }
        return ArtifactoryVersion.NOT_FOUND;
    }

    public JsonParser createJsonParser(InputStream in) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createJsonParser(in);
    }

    public JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    public String encodeUrl(String unescaped) {
        byte[] rawdata = URLCodec.encodeUrl(URI.allowed_query,
                org.apache.commons.codec.binary.StringUtils.getBytesUtf8(unescaped));
        return org.apache.commons.codec.binary.StringUtils.newStringUsAscii(rawdata);
    }

    public ArtifactoryUploadResponse upload(HttpPut httpPut, HttpEntity fileEntity) throws IOException {
        httpPut.setEntity(fileEntity);
        return execute(httpPut);
    }

    public ArtifactoryUploadResponse execute(HttpPut httpPut) throws IOException {
        HttpResponse response = getHttpClient().execute(httpPut);
        ArtifactoryUploadResponse artifactoryResponse = null;
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            InputStream content = entity.getContent();
            if (content != null) {
                try {
                    JsonParser parser = createJsonParser(content);
                    artifactoryResponse = parser.readValueAs(ArtifactoryUploadResponse.class);
                } catch (Exception e) {
                    log.error("Failed while reading the response from: " + httpPut, e);
                }
                finally {
                    content.close();
                }
            }
        }
        if (artifactoryResponse == null) {
            artifactoryResponse = new ArtifactoryUploadResponse();
        }
        artifactoryResponse.setStatusLine(statusLine);
        return artifactoryResponse;
    }
}