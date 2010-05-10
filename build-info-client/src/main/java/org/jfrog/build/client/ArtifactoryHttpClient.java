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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.StringTokenizer;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryHttpClient {

    private static final Log log = LogFactory.getLog(ArtifactoryHttpClient.class);

    public static final Version UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION = new Version("2.2.3");
    public static final Version NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION = new Version("2.2.4");
    public static final Version MINIMAL_ARTIFACTORY_VERSION = new Version("2.2.3");
    public static final String VERSION_INFO_URL = "/api/system/version";

    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 300;    // 5 Minutes in seconds

    private final String artifactoryUrl;
    private final String username;
    private final String password;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
    private ProxyConfiguration proxyConfiguration;

    private PreemptiveHttpClient deployClient;

    public ArtifactoryHttpClient(String artifactoryUrl, String username, String password) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        this.username = username;
        this.password = password;
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

    public Version getVersion() throws IOException {
        String versionUrl = artifactoryUrl + VERSION_INFO_URL;
        PreemptiveHttpClient client = getHttpClient();
        HttpGet httpGet = new HttpGet(versionUrl);
        HttpResponse response = client.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return Version.NOT_FOUND;
        }
        String version = "2.2.2";
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            InputStream content = httpEntity.getContent();
            JsonParser parser;
            try {
                parser = createJsonParser(content);
            } finally {
                if (content != null) {
                    content.close();
                }
            }
            httpEntity.consumeContent();
            JsonNode result = parser.readValueAsTree();
            log.debug("Version result: " + result);
            version = result.get("version").getTextValue();
        }
        return new Version(version);
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

    public String urlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    public StatusLine upload(HttpPut httpPut, HttpEntity fileEntity) throws IOException {
        httpPut.setEntity(fileEntity);
        HttpResponse response = getHttpClient().execute(httpPut);
        StatusLine statusLine = response.getStatusLine();
        if (response.getEntity() != null) {
            response.getEntity().consumeContent();
        }
        return statusLine;
    }

    public static class Version {
        static final Version NOT_FOUND = new Version("0.0.0");

        private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

        private final int[] numbers = {2, 2, 2};
        private boolean snapshot;

        Version(String version) {
            StringTokenizer stringTokenizer = new StringTokenizer(version, ".", false);
            try {
                numbers[0] = Integer.parseInt(stringTokenizer.nextToken());
                numbers[1] = Integer.parseInt(stringTokenizer.nextToken());
                String miniminor = stringTokenizer.nextToken();
                snapshot = miniminor.endsWith(SNAPSHOT_SUFFIX);
                if (snapshot) {
                    numbers[2] =
                            Integer.parseInt(miniminor.substring(0, miniminor.length() - SNAPSHOT_SUFFIX.length()));
                } else {
                    numbers[2] = Integer.parseInt(miniminor);
                }
            } catch (NumberFormatException nfe) {
                snapshot = true;
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                snapshot = true;
            }
        }

        boolean isSnapshot() {
            return snapshot;
        }

        @SuppressWarnings({"SimplifiableIfStatement"})
        boolean isAtLeast(Version version) {
            if (isSnapshot() || isNotFound()) {
                //Hack
                return true;
            }
            return weight() >= version.weight();
        }

        boolean isNotFound() {
            return weight() == NOT_FOUND.weight();
        }

        int weight() {
            return numbers[0] * 100 + numbers[1] * 10 + numbers[2];
        }

        @Override
        public String toString() {
            return numbers[0] + "." + numbers[1] + "." + numbers[2];
        }
    }
}