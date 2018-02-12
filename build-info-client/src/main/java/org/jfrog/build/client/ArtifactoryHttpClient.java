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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.util.URI;

import java.io.IOException;
import java.io.InputStream;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.binary.StringUtils.newStringUsAscii;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryHttpClient {

    public static final ArtifactoryVersion UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("2.2.3");
    public static final ArtifactoryVersion NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("2.2.4");
    public static final ArtifactoryVersion STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION =
            new ArtifactoryVersion("5.2.1");
    public static final ArtifactoryVersion MINIMAL_ARTIFACTORY_VERSION = new ArtifactoryVersion("2.2.3");
    public static final String VERSION_INFO_URL = "/api/system/version";
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;    // 5 Minutes in seconds
    public static final int DEFAULT_CONNECTION_RETRY = 3;
    private final Log log;
    private final String artifactoryUrl;
    private final String username;
    private final String password;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
    private int connectionRetries = DEFAULT_CONNECTION_RETRY;
    private ProxyConfiguration proxyConfiguration;

    private PreemptiveHttpClient deployClient;

    public ArtifactoryHttpClient(String artifactoryUrl, String username, String password, Log log) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        this.username = username;
        this.password = password;
        this.log = log;
    }

    public static String encodeUrl(String unescaped) {
        byte[] rawData = URLCodec.encodeUrl(URI.allowed_query, getBytesUtf8(unescaped));
        return newStringUsAscii(rawData);
    }

    public static String encodePath(String unescaped) {
        URLCodec codec = new URLCodec();
        String[] split = StringUtils.split(unescaped, "/");
        for (int i = 0; i < split.length; i++) {
            split[i] = newStringUsAscii(codec.encode(getBytesUtf8(split[i])));
            // codec.encode replaces spaces with '+', but we want to escape them to %20.
            split[i] = split[i].replaceAll("\\+", "%20");
        }
        return StringUtils.join(split, "/");
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
     * Max Retries to perform
     *
     * @param connectionRetries The number of max retries.
     */
    public void setConnectionRetries(int connectionRetries) {
        this.connectionRetries = connectionRetries;
    }

    public int getConnectionRetries() {
        return connectionRetries;
    }

    /**
     * Release all connection and cleanup resources.
     */
    public void close() {
        if (deployClient != null) {
            deployClient.close();
        }
    }

    public PreemptiveHttpClient getHttpClient() {
        return getHttpClient(connectionTimeout);
    }

    public PreemptiveHttpClient getHttpClient(int connectionTimeout) {
        if (deployClient == null) {
            deployClient = new PreemptiveHttpClient(username, password, connectionTimeout, proxyConfiguration, connectionRetries);
            deployClient.setLog(log);
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
                EntityUtils.consume(httpEntity);
            }
            return ArtifactoryVersion.NOT_FOUND;
        }
        if (statusCode != HttpStatus.SC_OK) {
            if (response.getEntity() != null) {
                EntityUtils.consume(response.getEntity());
            }
            throw new IOException(response.getStatusLine().getReasonPhrase());
        }
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            InputStream content = httpEntity.getContent();
            JsonParser parser;
            try {
                parser = createJsonParser(content);
                EntityUtils.consume(httpEntity);
                JsonNode result = parser.readValueAsTree();
                log.debug("Version result: " + result);
                String version = result.get("version").asText();
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

    public JsonParser createJsonParser(String content) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createJsonParser(content);
    }

    public JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    public ArtifactoryUploadResponse upload(HttpPut httpPut, HttpEntity fileEntity) throws IOException {
        httpPut.setEntity(fileEntity);
        return execute(httpPut);
    }

    public ArtifactoryUploadResponse execute(HttpPut httpPut) throws IOException {
        HttpResponse response = getHttpClient().execute(httpPut);

        ArtifactoryUploadResponse artifactoryResponse = null;
        if (response.getEntity() != null && response.getEntity().getContent() != null) {
            InputStream in = response.getEntity().getContent();
            String content = IOUtils.toString(in, "UTF-8");
            if (StringUtils.isNotEmpty(content)) {
                try {
                    JsonParser parser = createJsonParser(content);
                    artifactoryResponse = parser.readValueAs(ArtifactoryUploadResponse.class);
                } catch (Exception e) {
                    // Displays the response received from the client and the stacktrace in case an Exception caught.
                    log.info("Response received: \n\n" + content + "\n\n");
                    log.error("Failed while reading the response from: " + httpPut, e);
                } finally {
                    in.close();
                }
            }
        }

        if (artifactoryResponse == null) {
            artifactoryResponse = new ArtifactoryUploadResponse();
        }
        StatusLine statusLine = response.getStatusLine();
        artifactoryResponse.setStatusLine(statusLine);
        return artifactoryResponse;
    }
}
