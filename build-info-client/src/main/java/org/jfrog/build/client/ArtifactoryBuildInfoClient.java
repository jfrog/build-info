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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.FileChecksumCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Artifactory client to perform build info related tasks.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryBuildInfoClient {
    private static final Log log = LogFactory.getLog(ArtifactoryBuildInfoClient.class);

    private static final Version UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION = new Version("2.2.3");
    private static final Version MINIMAL_ARTIFACTORY_VERSION = new Version("2.2.3");

    private static final String LOCAL_REPOS_REST_URL = "/api/repositories?type=local";
    private static final String VIRTUAL_REPOS_REST_URL = "/api/repositories?type=virtual";
    private static final String BUILD_REST_RUL = "/api/build";
    private static final String VERSION_INFO_URL = "/api/system/version";
    private static final int DEFAULT_CONNECTION_TIMEOUT = 120;    // 2 Minutes in seconds

    private final String artifactoryUrl;
    private final String username;
    private final String password;
    private ProxyConfiguration proxyConfiguration;
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
    /**
     * The http client used for deploying artifacts and build info. Created and cached on the first deploy request.
     */
    private PreemptiveHttpClient deployClient;

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl) {
        this(artifactoryUrl, null, null);
    }

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     * @param username       Authentication username
     * @param password       Authentication password
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl, String username, String password) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        this.username = username;
        this.password = password;
    }

    /**
     * @return A list of local repositories available for deployment.
     * @throws IOException On any connection error
     */
    public List<String> getLocalRepositoriesKeys() throws IOException {
        List<String> repositories = new ArrayList<String>();
        PreemptiveHttpClient client = getHttpClient();

        String localReposUrl = artifactoryUrl + LOCAL_REPOS_REST_URL;
        log.debug("Requesting local repositories list from: " + localReposUrl);
        HttpGet httpget = new HttpGet(localReposUrl);
        HttpResponse response = client.execute(httpget);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Failed to obtain list of repositories: " + response.getStatusLine());
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
        return repositories;
    }

    /**
     * @return A list of local repositories available for deployment.
     * @throws IOException On any connection error
     */
    public List<String> getVirtualRepositoryKeys() throws IOException {
        List<String> repositories = new ArrayList<String>();
        PreemptiveHttpClient client = getHttpClient();

        String localReposUrl = artifactoryUrl + VIRTUAL_REPOS_REST_URL;
        log.debug("Requesting local repositories list from: " + localReposUrl);
        HttpGet httpget = new HttpGet(localReposUrl);
        HttpResponse response = client.execute(httpget);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Failed to obtain list of repositories: " + response.getStatusLine());
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
        return repositories;
    }

    /**
     * Sends build info to Artifactory.
     *
     * @param buildInfo The build info to send
     * @throws IOException On any connection error
     */
    public void sendBuildInfo(Build buildInfo) throws IOException {
        String url = artifactoryUrl + BUILD_REST_RUL;
        HttpPut httpPut = new HttpPut(url);
        String buildInfoJson = buildInfoToJsonString(buildInfo);
        StringEntity stringEntity = new StringEntity(buildInfoJson);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        httpPut.setEntity(stringEntity);
        log.info("Deploying build info to: " + url);
        HttpResponse response = getHttpClient().execute(httpPut);
        if (response.getEntity() != null) {
            response.getEntity().consumeContent();
        }
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throw new IOException("Failed to send build info: " + response.getStatusLine().getReasonPhrase());
        }
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
     * Deploys the artifact to the destination repository.
     *
     * @param details Details about the deployed artifact
     * @throws IOException On any connection error
     */
    public void deployArtifact(DeployDetails details) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder(artifactoryUrl);
        deploymentPathBuilder.append("/").append(details.targetRepository);
        if (!details.artifactPath.startsWith("/")) {
            deploymentPathBuilder.append("/");
        }
        deploymentPathBuilder.append(details.artifactPath);
        String deploymentPath = deploymentPathBuilder.toString();
        log.info("Deploying artifact: " + deploymentPath);
        uploadFile(details, deploymentPath);
        uploadChecksums(details, deploymentPath);
    }

    /**
     * Release all connection and cleanup resources.
     */
    public void shutdown() {
        if (deployClient != null) {
            deployClient.shutdown();
        }
    }

    private PreemptiveHttpClient getHttpClient() {
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

    private JsonParser createJsonParser(InputStream in) throws IOException {
        JsonFactory jsonFactory = createJsonFactory();
        return jsonFactory.createJsonParser(in);
    }

    private JsonFactory createJsonFactory() {
        JsonFactory jsonFactory = new JsonFactory();
        ObjectMapper mapper = new ObjectMapper(jsonFactory);
        mapper.getSerializationConfig().setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        mapper.getSerializationConfig().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        jsonFactory.setCodec(mapper);
        return jsonFactory;
    }

    private String buildInfoToJsonString(Build buildInfo) throws IOException {
        Version version = getVersion();

        boolean isCompatibleArtifactory = version.isAtLeast(MINIMAL_ARTIFACTORY_VERSION);
        if (!isCompatibleArtifactory) {
            log.warn("Note: Please upgrade your Artifactory server. This plugin is designed to work with version " +
                    MINIMAL_ARTIFACTORY_VERSION + " of Artifactory and above.");
        }
        //From Artifactory 2.2.3 we do need to discard new properties in order to avoid a server side exception
        //on JSON parsing. Our JSON writer is configured to discard null values.
        if (!isCompatibleArtifactory && !version.isAtLeast(UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION)) {
            buildInfo.setBuildAgent(null);
            buildInfo.setParentName(null);
            buildInfo.setParentNumber(null);
        }
        JsonFactory jsonFactory = createJsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeObject(buildInfo);
        String result = writer.getBuffer().toString();
        return result;
    }

    private Version getVersion() throws IOException {
        String versionUrl = artifactoryUrl + VERSION_INFO_URL;
        PreemptiveHttpClient client = getHttpClient();
        HttpGet httpGet = new HttpGet(versionUrl);
        HttpResponse response = client.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return Version.UNKNOWN;
        }
        String version = "2.2.2";
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            JsonParser parser = createJsonParser(httpEntity.getContent());
            httpEntity.consumeContent();
            JsonNode result = parser.readValueAsTree();
            log.debug("Version result: " + result);
            version = result.get("version").getTextValue();
        }
        return new Version(version);
    }

    private String urlEncode(String value) throws UnsupportedEncodingException {
        return URLEncoder.encode(value, "UTF-8");
    }

    private void uploadFile(DeployDetails details, String uploadUrl) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder().append(uploadUrl);
        if (details.properties != null) {
            for (Map.Entry<String, String> property : details.properties.entrySet()) {
                deploymentPathBuilder.append(";").append(urlEncode(property.getKey()))
                        .append("=").append(urlEncode(property.getValue()));
            }
        }
        HttpPut httpPut = new HttpPut(deploymentPathBuilder.toString());
        FileEntity fileEntity = new FileEntity(details.file, "binary/octet-stream");
        StatusLine statusLine = upload(httpPut, fileEntity);
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Failed to deploy file: " + statusLine.getReasonPhrase());
        }
    }

    public void uploadChecksums(DeployDetails details, String uploadUrl) throws IOException {
        Map<String, String> checksums = getChecksumMap(details);
        String fileAbsolutePath = details.file.getAbsolutePath();
        String md5 = checksums.get("MD5");
        if (StringUtils.isNotBlank(md5)) {
            log.debug("Uploading MD5 for file " + fileAbsolutePath + " : " + md5);
            HttpPut putMd5 = new HttpPut(uploadUrl + ".md5");
            StringEntity md5StringEntity = new StringEntity(md5);
            StatusLine md5StatusLine = upload(putMd5, md5StringEntity);
            if (md5StatusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Failed to deploy MD5 checksum: " + md5StatusLine.getReasonPhrase());
            }
        }
        String sha1 = checksums.get("SHA1");
        if (StringUtils.isNotBlank(sha1)) {
            log.debug("Uploading SHA1 for file " + fileAbsolutePath + " : " + sha1);
            HttpPut putSha1 = new HttpPut(uploadUrl + ".sha1");
            StringEntity sha1StringEntity = new StringEntity(sha1);
            StatusLine sha1StatusLine = upload(putSha1, sha1StringEntity);
            if (sha1StatusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException("Failed to deploy SHA1 checksum: " + sha1StatusLine.getReasonPhrase());
            }
        }
    }

    private Map<String, String> getChecksumMap(DeployDetails details) throws IOException {
        Map<String, String> checksums = Maps.newHashMap();

        List<String> checksumTypeList = Lists.newArrayList();

        if (StringUtils.isBlank(details.md5)) {
            checksumTypeList.add("MD5");
        } else {
            checksums.put("MD5", details.md5);
        }

        if (StringUtils.isBlank(details.sha1)) {
            checksumTypeList.add("SHA1");
        } else {
            checksums.put("SHA1", details.sha1);
        }

        if (!checksumTypeList.isEmpty()) {
            try {
                checksums.putAll(FileChecksumCalculator.calculateChecksums(details.file,
                        checksumTypeList.toArray(new String[checksumTypeList.size()])));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }
        return checksums;
    }

    private StatusLine upload(HttpPut httpPut, HttpEntity fileEntity) throws IOException {
        httpPut.setEntity(fileEntity);
        HttpResponse response = getHttpClient().execute(httpPut);
        StatusLine statusLine = response.getStatusLine();
        if (response.getEntity() != null) {
            response.getEntity().consumeContent();
        }
        return statusLine;
    }

    static class Version {
        static final Version UNKNOWN = new Version("0.0.0");

        private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

        private final int[] numbers = {2, 2, 2};
        private final boolean snapshot;

        Version(String version) {
            StringTokenizer stringTokenizer = new StringTokenizer(version, ".", false);
            if (stringTokenizer.countTokens() == 3) {
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
            } else {
                throw new IllegalArgumentException("Version is expected to be in the format major.minor.miniminor");
            }
        }

        boolean isSnapshot() {
            return snapshot;
        }

        @SuppressWarnings({"SimplifiableIfStatement"})
        boolean isAtLeast(Version version) {
            if (isSnapshot() || isUnknown()) {
                //Hack
                return true;
            }
            return weight() >= version.weight();
        }

        boolean isUnknown() {
            return weight() == UNKNOWN.weight();
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
