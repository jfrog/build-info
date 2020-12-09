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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.api.release.BintrayUploadInfoOverride;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.*;
import org.jfrog.build.client.bintrayResponse.BintrayResponse;
import org.jfrog.build.client.bintrayResponse.BintrayResponseFactory;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DeploymentUrlUtils;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;
import org.jfrog.build.extractor.usageReport.UsageReporter;
import org.jfrog.build.util.VersionCompatibilityType;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.jfrog.build.client.ArtifactoryHttpClient.encodeUrl;
import static org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration.DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB;

/**
 * Artifactory client to perform build info related tasks.
 *
 * @author Yossi Shaul
 */
public class ArtifactoryBuildInfoClient extends ArtifactoryBaseClient implements AutoCloseable {
    private static final String LOCAL_REPOS_REST_URL = "/api/repositories?type=local";
    private static final String REMOTE_REPOS_REST_URL = "/api/repositories?type=remote";
    private static final String VIRTUAL_REPOS_REST_URL = "/api/repositories?type=virtual";
    private static final String PUSH_TO_BINTRAY_REST_URL = "/api/build/pushToBintray/";
    private static final String BUILD_REST_URL = "/api/build";
    private static final String BUILD_RETENTION_REST_URL = BUILD_REST_URL + "/retention/";
    private static final String BUILD_RETENTION_REST_ASYNC_PARAM = "?async=";
    public static final String BUILD_BROWSE_URL = "/webapp/builds";
    public static final String APPLICATION_VND_ORG_JFROG_ARTIFACTORY_JSON = "application/vnd.org.jfrog.artifactory+json";
    public static final String APPLICATION_JSON = "application/json";
    public static final String ITEM_LAST_MODIFIED = "/api/storage/";
    private static final String USAGE_API = "/api/system/usage";
    private static final ArtifactoryVersion USAGE_ARTIFACTORY_MIN_VERSION = new ArtifactoryVersion("6.9.0");

    private int minChecksumDeploySizeKb = DEFAULT_MIN_CHECKSUM_DEPLOY_SIZE_KB;

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl, Log log) {
        this(artifactoryUrl, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, log);
    }

    /**
     * Creates a new client for the given Artifactory url.
     *
     * @param artifactoryUrl Artifactory url in the form of: protocol://host[:port]/contextPath
     * @param username       Authentication username
     * @param password       Authentication password
     */
    public ArtifactoryBuildInfoClient(String artifactoryUrl, String username, String password, String accessToken, Log log) {
        super(artifactoryUrl, username, password, accessToken, log);
    }

    public ArtifactoryBuildInfoClient(String artifactoryUrl, String username, String password, Log log) {
        this(artifactoryUrl, username, password, StringUtils.EMPTY, log);
    }

    /**
     * @return A list of local repositories available for deployment.
     * @throws IOException On any connection error
     */
    public List<String> getLocalRepositoriesKeys() throws IOException {
        return getRepositoriesList(LOCAL_REPOS_REST_URL);
    }

    /**
     * Set min file size for checksum deployment.
     * @param minChecksumDeploySizeKb - file size in KB
     */
    public void setMinChecksumDeploySizeKb(int minChecksumDeploySizeKb) {
        this.minChecksumDeploySizeKb = minChecksumDeploySizeKb;
    }

    /**
     * @return A list of local and cache repositories.
     * @throws IOException On any connection error
     */
    public List<String> getLocalAndCacheRepositoriesKeys() throws IOException {
        List<String> localRepositoriesKeys = getLocalRepositoriesKeys();
        List<String> remoteRepositories = getRemoteRepositoriesKeys();
        List<String> cacheRepositories = CommonUtils.transformList(remoteRepositories, repoKey -> repoKey + "-cache");

        return CommonUtils.concatLists(localRepositoriesKeys, cacheRepositories);
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
            throw new IOException("Failed to obtain list of repositories. Status code: " + statusLine.getStatusCode() + httpClient.getMessageFromEntity(entity));
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
                        String repositoryKey = jsonNode.get("key").asText();
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
        String url = String.format("%s%s", artifactoryUrl, BUILD_REST_URL);
        HttpPut httpPut = new HttpPut(url);
        try {
            log.info("Deploying build descriptor to: " + httpPut.getURI().toString());
            sendHttpEntityRequest(httpPut, buildInfoJson, APPLICATION_VND_ORG_JFROG_ARTIFACTORY_JSON);
        } catch (IOException e) {
            throw new IOException("Failed to send build descriptor. " + e.getMessage(), e);
        }
    }

    public Build getBuildInfo(String buildName, String buildNumber) throws IOException {
        // Only If the value of the buildNumber sent is "LATEST" or "LAST_RELEASE", replace the value with a specific build number.
        buildNumber = getLatestBuildNumberFromArtifactory(buildName, buildNumber);
        if (buildNumber == null) {
            return null;
        }

        String url = String.format("%s%s/%s/%s", artifactoryUrl, BUILD_REST_URL, encodeUrl(buildName), encodeUrl(buildNumber));
        HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse httpResponse = sendHttpRequest(httpGet, HttpStatus.SC_OK);
            String buildInfoJson = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");
            return getBuildFromJson(buildInfoJson);
        } catch (IOException e) {
            throw new IOException("Failed to get build info. " + e.getMessage(), e);
        }
    }

    private String getLatestBuildNumberFromArtifactory(String buildName, String buildNumber) throws IOException {
        ArtifactoryDependenciesClient dependenciesClient = new ArtifactoryDependenciesClient(artifactoryUrl, httpClient, log);
        return dependenciesClient.getLatestBuildNumberFromArtifactory(buildName, buildNumber);
    }

    public void sendBuildRetetion(BuildRetention buildRetention, String buildName, boolean async) throws IOException {
        String buildRetentionJson = toJsonString(buildRetention);
        String url = artifactoryUrl + BUILD_RETENTION_REST_URL + buildName + BUILD_RETENTION_REST_ASYNC_PARAM + async;
        HttpPost httpPost = new HttpPost(url);
        try {
            log.info(createBuildRetentionLogMsg(buildRetention, async));
            log.debug(buildRetentionJson);
            sendHttpEntityRequest(httpPost, buildRetentionJson, APPLICATION_JSON);
        } catch (IOException e) {
            log.error("Failed to execute build retention.", e);
            throw new IOException("Failed to execute build retention: " + e.getMessage(), e);
        }
    }

    private String createBuildRetentionLogMsg(BuildRetention buildRetention, boolean async) {
        StringBuilder strBuilder = new StringBuilder().append("Sending");

        if (async) {
            strBuilder.append(" async");
        }

        strBuilder.append(" request for build retention");

        if (buildRetention.isDeleteBuildArtifacts()) {
            strBuilder.append(", deleting build artifacts");
        }

        if (buildRetention.getCount() != -1) {
            strBuilder.append(", max number of builds to store: ").append(buildRetention.getCount());
        }

        if (buildRetention.getMinimumBuildDate() != null) {
            strBuilder.append(", min build date: ").append(buildRetention.getMinimumBuildDate());
        }

        if (!buildRetention.getBuildNumbersNotToBeDiscarded().isEmpty()) {
            strBuilder.append(", build numbers not to be discarded: ").append(buildRetention.getBuildNumbersNotToBeDiscarded());
        }
        strBuilder.append(".");

        return strBuilder.toString();
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
            log.info("Deploying build descriptor to: " + httpPost.getURI().toString());
            sendHttpEntityRequest(httpPost, modulesAsJsonString, APPLICATION_VND_ORG_JFROG_ARTIFACTORY_JSON);
        } catch (Exception e) {
            log.error("Could not build the build-info modules object.", e);
            throw new IOException("Could not publish build-info modules: " + e.getMessage(), e);
        }
    }

    private void sendHttpEntityRequest(HttpEntityEnclosingRequestBase request, String content, String contentType) throws IOException {
        StringEntity stringEntity = new StringEntity(content, "UTF-8");
        stringEntity.setContentType(contentType);
        request.setEntity(stringEntity);
        sendHttpRequest(request, HttpStatus.SC_CREATED, HttpStatus.SC_OK, HttpStatus.SC_NO_CONTENT);
    }

    private HttpResponse sendHttpRequest(HttpUriRequest request, int... httpStatuses) throws IOException {
        HttpResponse httpResponse;
        httpResponse = httpClient.getHttpClient().execute(request);
        StatusLine statusLine = httpResponse.getStatusLine();
        for (int status : httpStatuses) {
            if (statusLine.getStatusCode() == status) {
                return httpResponse;
            }
        }

        HttpEntity responseEntity = httpResponse.getEntity();
        throw new IOException(statusLine.getStatusCode() + httpClient.getMessageFromEntity(responseEntity));
    }

    public ItemLastModified getItemLastModified(String path) throws IOException {
        String url = artifactoryUrl + ITEM_LAST_MODIFIED + path + "?lastModified";
        HttpGet get = new HttpGet(url);
        HttpResponse response = httpClient.getHttpClient().execute(get);
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new IOException("While requesting item info for path " + path + " received " + response.getStatusLine().getStatusCode() + ":" + httpClient.getMessageFromEntity(response.getEntity()));
        }
        HttpEntity httpEntity = response.getEntity();
        if (httpEntity != null) {
            try (InputStream content = httpEntity.getContent()) {
                JsonNode result = httpClient.getJsonNode(content);
                JsonNode lastModified = result.get("lastModified");
                JsonNode uri = result.get("uri");
                if (lastModified == null || uri == null) {
                    throw new IOException("Unexpected JSON response when requesting info for path " + path + httpClient.getMessageFromEntity(response.getEntity()));
                }

                return new ItemLastModified(uri.asText(), lastModified.asText());
            } finally {
                EntityUtils.consume(httpEntity);
            }
        }
        throw new IOException("The path " + path + " returned empty entity");
    }

    /**
     * Deploys the artifact to the destination repository.
     *
     * @param details Details about the deployed artifact
     * @return ArtifactoryResponse The response content received from Artifactory
     * @throws IOException On any connection error
     */
    public ArtifactoryUploadResponse deployArtifact(DeployDetails details) throws IOException {
        return deployArtifact(details, null);
    }

    /**
     * Deploys the artifact to the destination repository, with addition of prefix to the printed log.
     *
     * @param details   Details about the deployed artifact
     * @param logPrefix Will be printed as a log-prefix
     * @return
     * @throws IOException On any connection error
     */
    public ArtifactoryUploadResponse deployArtifact(DeployDetails details, String logPrefix) throws IOException {
        String deploymentPath = buildDeploymentPath(details);
        logPrefix = logPrefix == null ? "" : logPrefix + " ";
        log.info(logPrefix + "Deploying artifact: " + deploymentPath);
        return doDeployArtifact(details, deploymentPath);
    }

    private String buildDeploymentPath(DeployDetails details) throws IOException {
        List<String> pathComponents = new ArrayList<String>();
        pathComponents.add(encodeUrl(artifactoryUrl));
        pathComponents.add(DeploymentUrlUtils.encodePath(details.getTargetRepository()));
        pathComponents.add(DeploymentUrlUtils.encodePath(details.getArtifactPath()));
        return StringUtils.join(pathComponents, "/");
    }

    private ArtifactoryUploadResponse doDeployArtifact(DeployDetails details, String deploymentPath) throws IOException {
        ArtifactoryUploadResponse response = uploadFile(details, deploymentPath);
        // Artifactory 2.3.2+ will take the checksum from the headers of the put request for the file
        if (!getArtifactoryVersion().isAtLeast(new ArtifactoryVersion("2.3.2"))) {
            uploadChecksums(details, deploymentPath);
        }
        return response;
    }

    /**
     * @return Artifactory version if working against a compatible version of Artifactory
     */
    public ArtifactoryVersion verifyCompatibleArtifactoryVersion() throws VersionException {
        ArtifactoryVersion version;
        try {
            version = httpClient.getVersion();
        } catch (IOException e) {
            throw new VersionException("Error occurred while requesting version information: " + e.getCause(), e,
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

        if (StringUtils.isNotEmpty(passphrase)) {
            urlBuilder.setParameter("gpgPassphrase", passphrase);
        }
        if (!StringUtils.equals(signMethod, "descriptor")) {
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
        JsonParser parser = httpClient.createJsonFactory().createParser(content);
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

    public HttpResponse distributeBuild(String buildName, String buildNumber, Distribution promotion) throws IOException {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for distribution.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for distribution.");
        }

        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append(BUILD_REST_URL).append("/distribute/").
                append(encodeUrl(buildName)).append("/").append(encodeUrl(buildNumber));

        String distributionJson = toJsonString(promotion);

        HttpPost httpPost = new HttpPost(urlBuilder.toString());

        StringEntity stringEntity = new StringEntity(distributionJson);
        stringEntity.setContentType("application/json");
        httpPost.setEntity(stringEntity);

        log.info("Distributing build " + buildName + ", #" + buildNumber);
        return httpClient.getHttpClient().execute(httpPost);
    }

    public Map<String, List<Map>> getUserPluginInfo() throws IOException {
        String url = new StringBuilder(artifactoryUrl).append("/api/plugins").toString();
        HttpGet getPlugins = new HttpGet(url);
        HttpResponse getResponse = httpClient.getHttpClient().execute(getPlugins);
        StatusLine statusLine = getResponse.getStatusLine();
        HttpEntity responseEntity = getResponse.getEntity();
        if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
            throw new IOException("Failed to obtain user plugin information. Status code: " + statusLine.getStatusCode() + httpClient.getMessageFromEntity(responseEntity));
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
        return new HashMap<>();
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
            throw new IOException("Failed to obtain staging strategy. Status code: " + statusLine.getStatusCode() + httpClient.getMessageFromEntity(responseEntity));
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
        return new HashMap<>();
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

    public HttpResponse executeUpdateFileProperty(String itemPath, String properties) throws IOException {
        StringBuilder urlBuilder = new StringBuilder(artifactoryUrl).append("/api/storage/")
                .append(encodeUrl(itemPath)).append("?").append("properties=").append(encodeUrl(properties));

        HttpPut postRequest = new HttpPut(urlBuilder.toString());
        return httpClient.getHttpClient().execute(postRequest);
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

    private Build getBuildFromJson(String buildInfoJson) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(buildInfoJson);
        return mapper.readValue(actualObj.get("buildInfo").toString(), Build.class);
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

        if (details.isExplode()) {
            httpPut.addHeader("X-Explode-Archive", "true");
        }

        FileEntity fileEntity = new FileEntity(details.getFile(), "binary/octet-stream");

        response = httpClient.upload(httpPut, fileEntity);
        int statusCode = response.getStatusLine().getStatusCode();

        //Accept both 200, and 201 for backwards-compatibility reasons
        if ((statusCode != HttpStatus.SC_CREATED) && (statusCode != HttpStatus.SC_OK)) {
            throw new IOException("Failed to deploy file. Status code: " + statusCode + getMessage(response));
        }

        return response;
    }

    private ArtifactoryUploadResponse tryChecksumDeploy(DeployDetails details, String uploadUrl) throws UnsupportedEncodingException {
        // Try checksum deploy only on file size equal or greater than 'minChecksumDeploySizeKb'
        long fileLength = details.getFile().length();
        if (fileLength < minChecksumDeploySizeKb * 1024) {
            log.debug("Skipping checksum deploy of file size " + fileLength + " bytes, falling back to regular deployment.");
            return null;
        }

        if (details.isExplode()) {
            log.debug("Skipping checksum deploy due to explode file request.");
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
        deploymentPathBuilder.append(DeploymentUrlUtils.buildMatrixParamsString(details.getProperties(), true));
        HttpPut httpPut = new HttpPut(deploymentPathBuilder.toString());
        httpPut.addHeader(SHA1_HEADER_NAME, details.getSha1());
        httpPut.addHeader(MD5_HEADER_NAME, details.getMd5());
        log.debug("Full Artifact Http path: " + httpPut.toString() + "\n@Http Headers: " + Arrays.toString(httpPut.getAllHeaders()));
        return httpPut;
    }

    public void uploadChecksums(DeployDetails details, String uploadUrl) throws IOException {
        Map<String, String> checksums = getChecksumMap(details);
        String fileAbsolutePath = details.getFile().getAbsolutePath();
        String sha1 = checksums.get("SHA1");
        if (StringUtils.isNotBlank(sha1)) {
            log.debug("Uploading SHA1 for file " + fileAbsolutePath + " : " + sha1);
            String sha1Url = uploadUrl + ".sha1" + DeploymentUrlUtils.buildMatrixParamsString(details.getProperties(), true);
            HttpPut putSha1 = new HttpPut(sha1Url);
            StringEntity sha1StringEntity = new StringEntity(sha1);
            ArtifactoryUploadResponse response = httpClient.upload(putSha1, sha1StringEntity);
            StatusLine sha1StatusLine = response.getStatusLine();
            int sha1StatusCode = sha1StatusLine.getStatusCode();

            //Accept both 200, and 201 for backwards-compatibility reasons
            if ((sha1StatusCode != HttpStatus.SC_CREATED) && (sha1StatusCode != HttpStatus.SC_OK)) {
                throw new IOException("Failed to deploy SHA1 checksum. Status code: " + sha1StatusCode + getMessage(response));
            }
        }
        String md5 = checksums.get("MD5");
        if (StringUtils.isNotBlank(md5)) {
            log.debug("Uploading MD5 for file " + fileAbsolutePath + " : " + md5);
            String md5Url = uploadUrl + ".md5" + DeploymentUrlUtils.buildMatrixParamsString(details.getProperties(), true);
            HttpPut putMd5 = new HttpPut(md5Url);
            StringEntity md5StringEntity = new StringEntity(md5);
            ArtifactoryUploadResponse response = httpClient.upload(putMd5, md5StringEntity);
            StatusLine md5StatusLine = response.getStatusLine();
            int md5StatusCode = md5StatusLine.getStatusCode();

            //Accept both 200, and 201 for backwards-compatibility reasons
            if ((md5StatusCode != HttpStatus.SC_CREATED) && (md5StatusCode != HttpStatus.SC_OK)) {
                throw new IOException("Failed to deploy MD5 checksum. Status code: " + md5StatusCode + getMessage(response));
            }
        }
    }

    private Map<String, String> getChecksumMap(DeployDetails details) throws IOException {
        Map<String, String> checksums = new HashMap<>();

        List<String> checksumTypeList = new ArrayList<>();

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

    /**
     * Returns the response message
     *
     * @param response from Artifactory
     * @return the response message String.
     */
    private String getMessage(ArtifactoryUploadResponse response) {
        String responseMessage = "";
        if (response.getErrors() != null) {
            responseMessage = getResponseMessage(response.getErrors());
            if (StringUtils.isNotBlank(responseMessage)) {
                responseMessage = " Response message: " + responseMessage;
            }
        }
        return responseMessage;
    }

    /**
     * @param errorList errors list returned from Artifactory.
     * @return response message string.
     */
    private String getResponseMessage(List<ArtifactoryUploadResponse.Error> errorList) {
        if (errorList == null || errorList.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("Artifactory returned the following errors: ");
        for (ArtifactoryUploadResponse.Error error : errorList) {
            builder.append("\n").
                    append(error.getMessage()).
                    append(" Status code: ").
                    append(error.getStatus());
        }
        return builder.toString();
    }

    public void reportUsage(UsageReporter usageReporter) throws IOException {
        // Check Artifactory supported version.
        ArtifactoryVersion version = getArtifactoryVersion();
        if (version.isNotFound()) {
            throw new IOException("Could not get Artifactory version.");
        }
        if (!version.isAtLeast(USAGE_ARTIFACTORY_MIN_VERSION)) {
            throw new IOException("Usage report is not supported on targeted Artifactory server.");
        }

        // Create request.
        String url = artifactoryUrl + USAGE_API;
        String encodedUrl = ArtifactoryHttpClient.encodeUrl(url);
        String requestBody = new JsonSerializer<UsageReporter>().toJSON(usageReporter);
        StringEntity entity = new StringEntity(requestBody, "UTF-8");
        entity.setContentType("application/json");
        HttpPost request = new HttpPost(encodedUrl);
        request.setEntity(entity);

        // Send request
        HttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.getHttpClient().execute(request);
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() != HttpStatus.SC_OK) {
                throw new IOException(String.format("Artifactory response: %s %s", statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
        } finally {
            if (httpResponse != null) {
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
        }
    }
}
