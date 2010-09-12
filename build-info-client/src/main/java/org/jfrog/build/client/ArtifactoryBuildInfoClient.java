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
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.client.ArtifactoryHttpClient.*;

/**
 * Artifactory client to perform build info related tasks.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryBuildInfoClient {
    private final Log log;

    private static final String LOCAL_REPOS_REST_URL = "/api/repositories?type=local";
    private static final String VIRTUAL_REPOS_REST_URL = "/api/repositories?type=virtual";
    private static final String BUILD_REST_RUL = "/api/build";

    /**
     * The http client used for deploying artifacts and build info. Created and cached on the first deploy request.
     */
    private ArtifactoryHttpClient httpClient;
    private String artifactoryUrl;

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl, Log log) {
        this(artifactoryUrl, null, null, log);
    }

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     * @param username       Authentication username
     * @param password       Authentication password
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl, String username, String password, Log log) {
        this.artifactoryUrl = StringUtils.stripEnd(artifactoryUrl, "/");
        httpClient = new ArtifactoryHttpClient(this.artifactoryUrl, username, password, log);
        this.log = log;
    }

    /**
     * Network timeout in seconds to use both for connection establishment and for unanswered requests.
     *
     * @param connectionTimeout Timeout in seconds.
     */
    public void setConnectionTimeout(int connectionTimeout) {
        httpClient.setConnectionTimeout(connectionTimeout);
    }

    /**
     * Sets the proxy host and port.
     *
     * @param host Proxy host
     * @param port Proxy port
     */
    public void setProxyConfiguration(String host, int port) {
        httpClient.setProxyConfiguration(host, port, null, null);
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
        httpClient.setProxyConfiguration(host, port, username, password);
    }

    /**
     * @return A list of local repositories available for deployment.
     * @throws IOException On any connection error
     */
    public List<String> getLocalRepositoriesKeys() throws IOException {
        List<String> repositories = new ArrayList<String>();
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String localReposUrl = artifactoryUrl + LOCAL_REPOS_REST_URL;
        log.debug("Requesting local repositories list from: " + localReposUrl);
        HttpGet httpget = new HttpGet(localReposUrl);
        HttpResponse response = client.execute(httpget);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            throwHttpIOException("Failed to obtain list of repositories:", statusLine);
        } else {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                repositories = new ArrayList<String>();
                InputStream content = entity.getContent();
                JsonParser parser;
                try {
                    parser = httpClient.createJsonParser(content);
                    JsonNode result = parser.readValueAsTree();
                    log.debug("Repositories result = " + result);
                    for (JsonNode jsonNode : result) {
                        String repositoryKey = jsonNode.get("key").getTextValue();
                        repositories.add(repositoryKey);
                    }
                } finally {
                    if (content != null) {
                        content.close();
                    }
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
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String localReposUrl = artifactoryUrl + VIRTUAL_REPOS_REST_URL;
        log.debug("Requesting local repositories list from: " + localReposUrl);
        HttpGet httpget = new HttpGet(localReposUrl);
        HttpResponse response = client.execute(httpget);
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            throwHttpIOException("Failed to obtain list of repositories:", statusLine);
        } else {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                repositories = new ArrayList<String>();
                InputStream content = entity.getContent();
                JsonParser parser;
                try {
                    parser = httpClient.createJsonParser(content);
                    JsonNode result = parser.readValueAsTree();
                    log.debug("Repositories result = " + result);
                    for (JsonNode jsonNode : result) {
                        String repositoryKey = jsonNode.get("key").getTextValue();
                        repositories.add(repositoryKey);
                    }
                } finally {
                    if (content != null) {
                        content.close();
                    }
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
        String buildInfoJson;
        try {
            buildInfoJson = buildInfoToJsonString(buildInfo);
        } catch (Exception e) {
            log.error("Could not build the build-info object.", e);
            throw new IOException("Could not publish build-info: " + e.getMessage());
        }
        StringEntity stringEntity = new StringEntity(buildInfoJson);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        httpPut.setEntity(stringEntity);
        log.info("Deploying build info to: " + url);
        HttpResponse response = httpClient.getHttpClient().execute(httpPut);
        if (response.getEntity() != null) {
            response.getEntity().consumeContent();
        }
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throwHttpIOException("Failed to send build info:", statusLine);
        }
    }

    public String getItemLastModified(String path) throws IOException, ParseException {
        String url = artifactoryUrl + "/api/storage/" + path + "?lastModified&deep=1";
        HttpGet get = new HttpGet(url);
        HttpResponse response = httpClient.getHttpClient().execute(get);

        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                entity.consumeContent();
            }
            throwHttpIOException("Failed to obtain item info:", statusLine);
        } else {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream content = entity.getContent();
                JsonParser parser;
                try {
                    parser = httpClient.createJsonParser(content);
                    JsonNode result = parser.readValueAsTree();
                    return result.get("lastModified").getTextValue();
                } finally {
                    if (content != null) {
                        content.close();
                    }
                    entity.consumeContent();
                }
            }
        }
        return null;
    }

    /**
     * Deploys the artifact to the destination repository.
     *
     * @param details Details about the deployed artifact
     * @throws IOException On any connection error
     */
    public void deployArtifact(DeployDetails details) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder(artifactoryUrl);
        deploymentPathBuilder.append("/").append(details.getTargetRepository());
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
     * @return Artifactory version if working against a compatible version of Artifactory
     * @throws IOException If server not found or it doesn't answer to the version query or it is too old
     */
    public Version verifyCompatibleArtifactoryVersion() throws VersionException {
        Version version;
        try {
            version = httpClient.getVersion();
        } catch (IOException e) {
            throw new VersionException("Error occurred while requesting version information: " + e.getMessage(), e,
                    VersionCompatibilityType.NOT_FOUND);
        }
        if (version.isNotFound()) {
            throw new VersionException(
                    "There is either an incompatible or no instance of Artifactory at the provided URL.",
                    VersionCompatibilityType.NOT_FOUND);
        }
        boolean isCompatibleArtifactory = version.isAtLeast(MINIMAL_ARTIFACTORY_VERSION);
        if (!isCompatibleArtifactory) {
            throw new VersionException("This plugin is compatible with version " + MINIMAL_ARTIFACTORY_VERSION +
                    " of Artifactory and above. Please upgrade your Artifactory server!",
                    VersionCompatibilityType.INCOMPATIBLE);
        }
        return version;
    }

    /**
     * Release all connection and cleanup resources.
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
    }

    private String buildInfoToJsonString(Build buildInfo) throws Exception {
        Version version = verifyCompatibleArtifactoryVersion();
        //From Artifactory 2.2.3 we do not need to discard new properties in order to avoid a server side exception on
        //JSON parsing. Our JSON writer is configured to discard null values.
        if (!version.isAtLeast(UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION)) {
            buildInfo.setBuildAgent(null);
            buildInfo.setParentName(null);
            buildInfo.setParentNumber(null);
            buildInfo.setVcsRevision(null);
        }
        //From Artifactory 2.2.4 we also handle non-numeric build numbers
        if (!version.isAtLeast(NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION)) {
            String buildNumber = buildInfo.getNumber();
            verifyNonNumericBuildNumber(buildNumber);
            String parentBuildNumber = buildInfo.getParentNumber();
            verifyNonNumericBuildNumber(parentBuildNumber);
        }
        return toJsonString(buildInfo);
    }

    String toJsonString(Build buildInfo) throws IOException {
        JsonFactory jsonFactory = httpClient.createJsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeObject(buildInfo);
        String result = writer.getBuffer().toString();
        return result;
    }

    private void verifyNonNumericBuildNumber(String buildNumber) {
        if (buildNumber != null) {
            try {
                Long.parseLong(buildNumber);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Cannot handle build/parent build number: " + buildNumber +
                                ". Non-numeric build numbers are supported by Artifactory version " +
                                NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION +
                                " and above. Please upgrade your Artifactory or use numeric build numbers.");
            }
        }
    }


    private void uploadFile(DeployDetails details, String uploadUrl) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder().append(uploadUrl);
        if (details.properties != null) {
            for (Map.Entry<String, String> property : details.properties.entrySet()) {
                deploymentPathBuilder.append(";").append(httpClient.urlEncode(property.getKey()))
                        .append("=").append(httpClient.urlEncode(property.getValue()));
            }
        }
        HttpPut httpPut = new HttpPut(deploymentPathBuilder.toString());
        FileEntity fileEntity = new FileEntity(details.file, "binary/octet-stream");
        StatusLine statusLine = httpClient.upload(httpPut, fileEntity);
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            throwHttpIOException("Failed to deploy file:", statusLine);
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
            StatusLine md5StatusLine = httpClient.upload(putMd5, md5StringEntity);
            if (md5StatusLine.getStatusCode() != HttpStatus.SC_OK) {
                throwHttpIOException("Failed to deploy MD5 checksum:", md5StatusLine);
            }
        }
        String sha1 = checksums.get("SHA1");
        if (StringUtils.isNotBlank(sha1)) {
            log.debug("Uploading SHA1 for file " + fileAbsolutePath + " : " + sha1);
            HttpPut putSha1 = new HttpPut(uploadUrl + ".sha1");
            StringEntity sha1StringEntity = new StringEntity(sha1);
            StatusLine sha1StatusLine = httpClient.upload(putSha1, sha1StringEntity);
            if (sha1StatusLine.getStatusCode() != HttpStatus.SC_OK) {
                throwHttpIOException("Failed to deploy SHA1 checksum:", sha1StatusLine);
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

    private void throwHttpIOException(String message, StatusLine statusLine) throws IOException {
        String errorMessage = new StringBuilder(message).append(" HTTP response code: ").
                append(statusLine.getStatusCode()).append(". HTTP response message: ").
                append(statusLine.getReasonPhrase()).toString();
        throw new IOException(errorMessage);
    }
}