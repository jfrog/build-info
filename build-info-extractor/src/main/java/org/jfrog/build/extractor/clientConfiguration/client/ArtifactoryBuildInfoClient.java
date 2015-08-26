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

package org.jfrog.build.extractor.clientConfiguration.client;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.http.*;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.release.BintrayUploadInfoOverride;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.*;
import org.jfrog.build.client.bintrayResponse.BintrayResponse;
import org.jfrog.build.client.bintrayResponse.BintrayResponseFactory;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;

import static org.jfrog.build.client.ArtifactoryHttpClient.encodeUrl;

/**
 * Artifactory client to perform build info related tasks.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryBuildInfoClient {
    private static final String LOCAL_REPOS_REST_URL = "/api/repositories?type=local";
    private static final String REMOTE_REPOS_REST_URL = "/api/repositories?type=remote";
    private static final String VIRTUAL_REPOS_REST_URL = "/api/repositories?type=virtual";
    private static final String PUSH_TO_BINTRAY_REST_URL = "/api/build/pushToBintray/";
    private static final String BUILD_REST_URL = "/api/build";
    private static final String BUILD_BROWSE_URL = "/webapp/builds";
    private static final int CHECKSUM_DEPLOY_MIN_FILE_SIZE = 10240; // Try checksum deploy of files greater than 10KB
    private final Log log;
    /**
     * The http client used for deploying artifacts and build info. Created and cached on the first deploy request.
     */
    private ArtifactoryHttpClient httpClient;
    private String artifactoryUrl;
    /**
     * Version of Artifactory we work with.
     */
    private ArtifactoryVersion artifactoryVersion;

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
     * Sets full proxy details.
     *
     * @param proxy Proxy instance {@link org.jfrog.build.client.ProxyConfiguration}
     */
    public void setProxyConfiguration(ProxyConfiguration proxy) {
        httpClient.setProxyConfiguration(proxy.host, proxy.port, proxy.username, proxy.password);
    }

    /**
     * @return A list of local repositories available for deployment.
     * @throws IOException On any connection error
     */
    public List<String> getLocalRepositoriesKeys() throws IOException {
        return getRepositoriesList(LOCAL_REPOS_REST_URL);
    }

    /**
     * @return A list of local and cache repositories.
     * @throws IOException On any connection error
     */
    public List<String> getLocalAndCacheRepositoriesKeys() throws IOException {
        List<String> localRepositoriesKeys = getLocalRepositoriesKeys();
        List<String> remoteRepositories = getRemoteRepositoriesKeys();
        List<String> cacheRepositories = Lists.transform(remoteRepositories, new Function<String, String>() {
            @Override
            public String apply(String repoKey) {
                return repoKey + "-cache";
            }
        });

        return Lists.newArrayList(Iterables.concat(localRepositoriesKeys, cacheRepositories));
    }

    /**
     * @return A list of remote repositories.
     * @throws IOException On any connection error
     */
    public List<String> getRemoteRepositoriesKeys() throws IOException {
        return getRepositoriesList(REMOTE_REPOS_REST_URL);
    }

    /**
     * @return A list of virtual repositories available for resolution.
     * @throws IOException On any connection error
     */
    public List<String> getVirtualRepositoryKeys() throws IOException {
        return getRepositoriesList(VIRTUAL_REPOS_REST_URL);
    }

    private List<String> getRepositoriesList(String restUrl) throws IOException {
        List<String> repositories = new ArrayList<String>();
        PreemptiveHttpClient client = httpClient.getHttpClient();

        String reposUrl = artifactoryUrl + restUrl;
        log.debug("Requesting repositories list from: " + reposUrl);
        HttpGet httpget = new HttpGet(reposUrl);
        HttpResponse response = client.execute(httpget);
        StatusLine statusLine = response.getStatusLine();
        HttpEntity entity = response.getEntity();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            if (entity != null) {
                EntityUtils.consume(entity);
            }
            throwHttpIOException("Failed to obtain list of repositories:", statusLine);
        } else {
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

    public void sendBuildInfo(String buildInfoJson) throws IOException {
        String url = artifactoryUrl + BUILD_REST_URL;
        HttpPut httpPut = new HttpPut(url);
        sendDescriptor(httpPut, buildInfoJson);
    }

    /**
     * Sends build info to Artifactory.
     *
     * @param buildInfo The build info to send
     * @throws IOException On any connection error
     */
    public void sendBuildInfo(Build buildInfo) throws IOException {
        log.debug("Sending build info: " + buildInfo);
        try {
            sendBuildInfo(buildInfoToJsonString(buildInfo));
        } catch (Exception e) {
            log.error("Could not build the build-info object.", e);
            throw new IOException("Could not publish build-info: " + e.getMessage());
        }
        String url = artifactoryUrl +
                BUILD_BROWSE_URL + "/" + encodeUrl(buildInfo.getName()) + "/" + encodeUrl(buildInfo.getNumber());
        log.info("Build successfully deployed. Browse it in Artifactory under " + url);
    }

    public void sendModuleInfo(Build build) throws IOException {
        log.debug("Sending build info modules: " + build);
        try {
            String url = artifactoryUrl + BUILD_REST_URL + "/append/" + encodeUrl(build.getName()) + "/" +
                    encodeUrl(build.getNumber());
            HttpPost httpPost = new HttpPost(url);
            String modulesAsJsonString = toJsonString(build.getModules());
            sendDescriptor(httpPost, modulesAsJsonString);
        } catch (Exception e) {
            log.error("Could not build the build-info modules object.", e);
            throw new IOException("Could not publish build-info modules: " + e.getMessage());
        }
    }

    private void sendDescriptor(HttpEntityEnclosingRequestBase request, String content) throws IOException {
        StringEntity stringEntity = new StringEntity(content, "UTF-8");
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        request.setEntity(stringEntity);
        log.info("Deploying build descriptor to: " + request.getURI().toString());
        HttpResponse response = httpClient.getHttpClient().execute(request);
        if (response.getEntity() != null) {
            EntityUtils.consume(response.getEntity());
        }
        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            throwHttpIOException("Failed to send build descriptor :", statusLine);
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
                EntityUtils.consume(entity);
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
                    EntityUtils.consume(entity);
                }
            }
        }
        return null;
    }

    /**
     * Deploys the artifact to the destination repository.
     *
     * @param details Details about the deployed artifact
     * @return ArtifactoryResponse The response content received from Artifactory
     * @throws IOException On any connection error
     */
    public ArtifactoryUploadResponse deployArtifact(DeployDetails details) throws IOException {
        StringBuilder deploymentPathBuilder = new StringBuilder(artifactoryUrl);
        deploymentPathBuilder.append("/").append(details.getTargetRepository());
        if (!details.getArtifactPath().startsWith("/")) {
            deploymentPathBuilder.append("/");
        }
        deploymentPathBuilder.append(details.getArtifactPath());
        String deploymentPath = deploymentPathBuilder.toString();
        log.info("Deploying artifact: " + deploymentPath);
        deploymentPath = encodeUrl(deploymentPath);
        ArtifactoryUploadResponse response = uploadFile(details, deploymentPath);
        // Artifactory 2.3.2+ will take the checksum from the headers of the put request for the file
        if (!getArtifactoryVersion().isAtLeast(new ArtifactoryVersion("2.3.2"))) {
            uploadChecksums(details, deploymentPath);
        }

        return response;
    }

    /**
     * @return Artifactory version if working against a compatible version of Artifactory
     * @throws IOException If server not found or it doesn't answer to the version query or it is too old
     */
    public ArtifactoryVersion verifyCompatibleArtifactoryVersion() throws VersionException {
        ArtifactoryVersion version;
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
        boolean isCompatibleArtifactory = version.isAtLeast(ArtifactoryHttpClient.MINIMAL_ARTIFACTORY_VERSION);
        if (!isCompatibleArtifactory) {
            throw new VersionException("This plugin is compatible with version " + ArtifactoryHttpClient.MINIMAL_ARTIFACTORY_VERSION +
                    " of Artifactory and above. Please upgrade your Artifactory server!",
                    VersionCompatibilityType.INCOMPATIBLE);
        }
        return version;
    }

    /**
     * Push build to bintray
     *
     * @param buildName         name of the build to push
     * @param buildNumber       number of the build to push
     * @param signMethod        flags if this artifacts should be signed or not
     * @param passphrase        passphrase in case that the artifacts should be signed
     * @param bintrayUploadInfo request body which contains the upload info
     * @return http Response with the response outcome
     * @throws IOException On any connection error
     * @see org.jfrog.build.api.release.BintrayUploadInfoOverride;
     */
    public BintrayResponse pushToBintray(String buildName, String buildNumber, String signMethod, String passphrase,
                                         BintrayUploadInfoOverride bintrayUploadInfo) throws IOException, URISyntaxException {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }

        String requestUrl = createRequestUrl(buildName, buildNumber, signMethod, passphrase);
        String requestBody = createJsonRequestBody(bintrayUploadInfo);
        HttpPost httpPost = new HttpPost(requestUrl);
        StringEntity entity = new StringEntity(requestBody);
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        HttpResponse response = httpClient.getHttpClient().execute(httpPost);
        return parseResponse(response);
    }

    // create pushToBintray request Url
    private String createRequestUrl(String buildName, String buildNumber, String signMethod, String passphrase) throws URISyntaxException {
        URIBuilder urlBuilder = new URIBuilder();
        urlBuilder.setPath(artifactoryUrl + PUSH_TO_BINTRAY_REST_URL + buildName + "/" + buildNumber);

        if (StringUtils.isNotEmpty(passphrase)){
            urlBuilder.setParameter("gpgPassphrase", passphrase);
        }
        if (!StringUtils.equals(signMethod, "descriptor")){
            urlBuilder.setParameter("gpgSign", signMethod);
        }
        return urlBuilder.toString();
    }

    // create pushToBintray request body
    private String createJsonRequestBody(BintrayUploadInfoOverride info) throws IOException {
        String bintrayInfoJson;
        if (!info.isValid()) {
            // empty json body to use the descriptor file if exists
            bintrayInfoJson = "{}";
        } else {
            bintrayInfoJson = toJsonString(info);
        }
        return bintrayInfoJson;
    }

    private BintrayResponse parseResponse(HttpResponse response) throws IOException {
        InputStream content = response.getEntity().getContent();
        int status = response.getStatusLine().getStatusCode();
        JsonParser parser = httpClient.createJsonFactory().createJsonParser(content);
        BintrayResponse responseObject = BintrayResponseFactory.createResponse(status, parser);
        return responseObject;
    }

    public HttpResponse stageBuild(String buildName, String buildNumber, Promotion promotion) throws IOException {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }

        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append(BUILD_REST_URL).append("/promote/").
                append(encodeUrl(buildName)).append("/").append(encodeUrl(buildNumber));

        String promotionJson = toJsonString(promotion);

        HttpPost httpPost = new HttpPost(urlBuilder.toString());

        StringEntity stringEntity = new StringEntity(promotionJson);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory.build.PromotionRequest+json");
        httpPost.setEntity(stringEntity);

        log.info("Promoting build " + buildName + ", #" + buildNumber);
        return httpClient.getHttpClient().execute(httpPost);
    }

    public Map<String, List<Map>> getUserPluginInfo() throws IOException {
        String url = new StringBuilder(artifactoryUrl).append("/api/plugins").toString();
        HttpGet getPlugins = new HttpGet(url);
        HttpResponse getResponse = httpClient.getHttpClient().execute(getPlugins);
        StatusLine statusLine = getResponse.getStatusLine();
        HttpEntity responseEntity = getResponse.getEntity();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            if (responseEntity != null) {
                EntityUtils.consume(responseEntity);
            }
            throwHttpIOException("Failed to obtain user plugin information:", statusLine);
        } else {
            if (responseEntity != null) {
                InputStream content = responseEntity.getContent();
                JsonParser parser;
                try {
                    parser = httpClient.createJsonParser(content);
                    return parser.readValueAs(Map.class);
                } finally {
                    if (content != null) {
                        content.close();
                    }
                }
            }
        }
        return Maps.newHashMap();
    }

    public HttpResponse executeUserPlugin(String executionName, Map<String, String> requestParams) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append("/api/plugins/execute/")
                .append(executionName).append("?");
        appendParamsToUrl(requestParams, urlBuilder);
        HttpPost postRequest = new HttpPost(urlBuilder.toString());
        return httpClient.getHttpClient().execute(postRequest);
    }

    public Map getStagingStrategy(String strategyName, String buildName, Map<String, String> requestParams)
            throws IOException {
        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append("/api/plugins/build/staging/")
                .append(encodeUrl(strategyName)).append("?buildName=")
                .append(encodeUrl(buildName)).append("&");
        appendParamsToUrl(requestParams, urlBuilder);
        HttpGet getRequest = new HttpGet(urlBuilder.toString());
        HttpResponse response = httpClient.getHttpClient().execute(getRequest);
        StatusLine statusLine = response.getStatusLine();
        HttpEntity responseEntity = response.getEntity();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            if (responseEntity != null) {
                EntityUtils.consume(responseEntity);
            }
            throwHttpIOException("Failed to obtain staging strategy:", statusLine);
        } else {
            if (responseEntity != null) {
                InputStream content = responseEntity.getContent();
                JsonParser parser;
                try {
                    parser = httpClient.createJsonParser(content);
                    return parser.readValueAs(Map.class);
                } finally {
                    if (content != null) {
                        content.close();
                    }
                }
            }
        }
        return Maps.newHashMap();
    }

    public HttpResponse executePromotionUserPlugin(String promotionName, String buildName, String buildNumber,
                                                   Map<String, String> requestParams) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append("/api/plugins/build/promote/")
                .append(promotionName).append("/").append(encodeUrl(buildName)).append("/")
                .append(encodeUrl(buildNumber)).append("?");
        appendParamsToUrl(requestParams, urlBuilder);
        HttpPost postRequest = new HttpPost(urlBuilder.toString());
        return httpClient.getHttpClient().execute(postRequest);
    }

    /**
     * Release all connection and cleanup resources.
     */
    public void shutdown() {
        if (httpClient != null) {
            httpClient.shutdown();
        }
    }

    private void appendParamsToUrl(Map<String, String> requestParams, StringBuilder urlBuilder)
            throws UnsupportedEncodingException {
        if ((requestParams != null) && !requestParams.isEmpty()) {
            urlBuilder.append("params=");
            Iterator<Map.Entry<String, String>> paramEntryIterator = requestParams.entrySet().iterator();
            String encodedPipe = encodeUrl("|");
            while (paramEntryIterator.hasNext()) {
                Map.Entry<String, String> paramEntry = paramEntryIterator.next();
                urlBuilder.append(encodeUrl(paramEntry.getKey()));
                String paramValue = paramEntry.getValue();
                if (StringUtils.isNotBlank(paramValue)) {
                    urlBuilder.append("=").append(encodeUrl(paramValue));
                }

                if (paramEntryIterator.hasNext()) {

                    urlBuilder.append(encodedPipe);
                }
            }
        }
    }

    public String buildInfoToJsonString(Build buildInfo) throws Exception {
        ArtifactoryVersion version = verifyCompatibleArtifactoryVersion();
        //From Artifactory 2.2.3 we do not need to discard new properties in order to avoid a server side exception on
        //JSON parsing. Our JSON writer is configured to discard null values.
        if (!version.isAtLeast(ArtifactoryHttpClient.UNKNOWN_PROPERTIES_TOLERANT_ARTIFACTORY_VERSION)) {
            buildInfo.setBuildAgent(null);
            buildInfo.setParentName(null);
            buildInfo.setParentNumber(null);
            buildInfo.setVcsRevision(null);
        }
        //From Artifactory 2.2.4 we also handle non-numeric build numbers
        if (!version.isAtLeast(ArtifactoryHttpClient.NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION)) {
            String buildNumber = buildInfo.getNumber();
            verifyNonNumericBuildNumber(buildNumber);
            String parentBuildNumber = buildInfo.getParentNumber();
            verifyNonNumericBuildNumber(parentBuildNumber);
        }
        return toJsonString(buildInfo);
    }

    String toJsonString(Object object) throws IOException {
        JsonFactory jsonFactory = httpClient.createJsonFactory();
        StringWriter writer = new StringWriter();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(writer);
        jsonGenerator.useDefaultPrettyPrinter();
        jsonGenerator.writeObject(object);
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
                                ArtifactoryHttpClient.NON_NUMERIC_BUILD_NUMBERS_TOLERANT_ARTIFACTORY_VERSION +
                                " and above. Please upgrade your Artifactory or use numeric build numbers.");
            }
        }
    }

    private ArtifactoryUploadResponse uploadFile(DeployDetails details, String uploadUrl) throws IOException {
        ArtifactoryUploadResponse response = tryChecksumDeploy(details, uploadUrl);
        if (response != null) {
            // Checksum deploy was performed:
            return response;
        }

        HttpPut httpPut = createHttpPutMethod(details, uploadUrl);
        // add the 100 continue directive
        httpPut.addHeader(HTTP.EXPECT_DIRECTIVE, HTTP.EXPECT_CONTINUE);

        FileEntity fileEntity = new FileEntity(details.getFile(), "binary/octet-stream");

        response = httpClient.upload(httpPut, fileEntity);
        int statusCode = response.getStatusLine().getStatusCode();

        //Accept both 200, and 201 for backwards-compatibility reasons
        if ((statusCode != HttpStatus.SC_CREATED) && (statusCode != HttpStatus.SC_OK)) {
            throwHttpIOException("Failed to deploy file:", response.getStatusLine());
        }

        return response;
    }

    private ArtifactoryUploadResponse tryChecksumDeploy(DeployDetails details, String uploadUrl) throws UnsupportedEncodingException {
        // Try checksum deploy only on file size greater than CHECKSUM_DEPLOY_MIN_FILE_SIZE
        long fileLength = details.getFile().length();
        if (fileLength < CHECKSUM_DEPLOY_MIN_FILE_SIZE) {
            log.debug("Skipping checksum deploy of file size " + fileLength + " , falling back to regular deployment.");
            return null;
        }

        // Artifactory 2.5.1+ has efficient checksum deployment (checks if the artifact already exists by it's checksum)
        if (!getArtifactoryVersion().isAtLeast(new ArtifactoryVersion("2.5.1"))) {
            return null;
        }

        HttpPut httpPut = createHttpPutMethod(details, uploadUrl);
        // activate checksum deploy
        httpPut.addHeader("X-Checksum-Deploy", "true");

        String fileAbsolutePath = details.getFile().getAbsolutePath();
        try {
            ArtifactoryUploadResponse response = httpClient.execute(httpPut);
            int statusCode = response.getStatusLine().getStatusCode();

            //Accept both 200, and 201 for backwards-compatibility reasons
            if ((statusCode == HttpStatus.SC_CREATED) || (statusCode == HttpStatus.SC_OK)) {
                log.debug("Successfully performed checksum deploy of file " + fileAbsolutePath + " : " + details.getSha1());
                return response;
            } else {
                log.debug("Failed checksum deploy of checksum '" + details.getSha1() + "' with statusCode: " + statusCode);
            }
        } catch (IOException e) {
            log.debug("Failed artifact checksum deploy of file " + fileAbsolutePath + " : " + details.getSha1());
        }

        return null;
    }

    private HttpPut createHttpPutMethod(DeployDetails details, String uploadUrl) throws UnsupportedEncodingException {
        StringBuilder deploymentPathBuilder = new StringBuilder().append(uploadUrl);
        deploymentPathBuilder.append(DeploymentUrlUtils.buildMatrixParamsString(details.getProperties()));
        HttpPut httpPut = new HttpPut(deploymentPathBuilder.toString());
        httpPut.addHeader("X-Checksum-Sha1", details.getSha1());
        httpPut.addHeader("X-Checksum-Md5", details.getMd5());
        log.debug("Full Artifact Http path: " + httpPut.toString() + "\n@Http Headers: " + Arrays.toString(httpPut.getAllHeaders()));

        return httpPut;
    }

    public void uploadChecksums(DeployDetails details, String uploadUrl) throws IOException {
        Map<String, String> checksums = getChecksumMap(details);
        String fileAbsolutePath = details.getFile().getAbsolutePath();
        String sha1 = checksums.get("SHA1");
        if (StringUtils.isNotBlank(sha1)) {
            log.debug("Uploading SHA1 for file " + fileAbsolutePath + " : " + sha1);
            String sha1Url = uploadUrl + ".sha1" + DeploymentUrlUtils.buildMatrixParamsString(details.getProperties());
            HttpPut putSha1 = new HttpPut(sha1Url);
            StringEntity sha1StringEntity = new StringEntity(sha1);
            ArtifactoryUploadResponse response = httpClient.upload(putSha1, sha1StringEntity);
            StatusLine sha1StatusLine = response.getStatusLine();
            int sha1StatusCode = sha1StatusLine.getStatusCode();

            //Accept both 200, and 201 for backwards-compatibility reasons
            if ((sha1StatusCode != HttpStatus.SC_CREATED) && (sha1StatusCode != HttpStatus.SC_OK)) {
                throwHttpIOException("Failed to deploy SHA1 checksum:", sha1StatusLine);
            }
        }
        String md5 = checksums.get("MD5");
        if (StringUtils.isNotBlank(md5)) {
            log.debug("Uploading MD5 for file " + fileAbsolutePath + " : " + md5);
            String md5Url = uploadUrl + ".md5" + DeploymentUrlUtils.buildMatrixParamsString(details.getProperties());
            HttpPut putMd5 = new HttpPut(md5Url);
            StringEntity md5StringEntity = new StringEntity(md5);
            ArtifactoryUploadResponse response = httpClient.upload(putMd5, md5StringEntity);
            StatusLine md5StatusLine = response.getStatusLine();
            int md5StatusCode = md5StatusLine.getStatusCode();

            //Accept both 200, and 201 for backwards-compatibility reasons
            if ((md5StatusCode != HttpStatus.SC_CREATED) && (md5StatusCode != HttpStatus.SC_OK)) {
                throwHttpIOException("Failed to deploy MD5 checksum:", md5StatusLine);
            }
        }
    }

    private Map<String, String> getChecksumMap(DeployDetails details) throws IOException {
        Map<String, String> checksums = Maps.newHashMap();

        List<String> checksumTypeList = Lists.newArrayList();

        if (StringUtils.isBlank(details.getMd5())) {
            checksumTypeList.add("MD5");
        } else {
            checksums.put("MD5", details.getMd5());
        }

        if (StringUtils.isBlank(details.getSha1())) {
            checksumTypeList.add("SHA1");
        } else {
            checksums.put("SHA1", details.getSha1());
        }

        if (!checksumTypeList.isEmpty()) {
            try {
                checksums.putAll(FileChecksumCalculator.calculateChecksums(details.getFile(),
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

    private ArtifactoryVersion getArtifactoryVersion() {
        if (artifactoryVersion == null) {
            try {
                artifactoryVersion = httpClient.getVersion();
            } catch (IOException e) {
                artifactoryVersion = ArtifactoryVersion.NOT_FOUND;
            }
        }
        return artifactoryVersion;
    }
}
