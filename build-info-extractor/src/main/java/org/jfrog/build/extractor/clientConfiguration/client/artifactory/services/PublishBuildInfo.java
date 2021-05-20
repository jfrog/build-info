package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class PublishBuildInfo extends VoidJFrogService {
    public static final String BUILD_BROWSE_URL = "/webapp/builds";
    private static final String BUILD_REST_URL = "/api/build";
    public static final String BUILD_BROWSE_PLATFORM_URL = "/ui/builds";
    private static final String BUILD_PROJECT_PARAM = "?project=";

    Build buildInfo;
    private final String platformUrl;
    private String buildInfoJson;

    public PublishBuildInfo(Build buildInfo, String platformUrl, Log logger) {
        super(logger);
        this.buildInfo = buildInfo;
        this.platformUrl = platformUrl;
    }

    // Returns the name of the build-info repository, corresponding to the project key sent.
    // Returns an empty string, if the provided projectKey is empty.
    public static String getProjectQueryParam(String project, String prefix) {
        if (StringUtils.isNotEmpty(project)) {
            return prefix + encodeUrl(project);
        }
        return "";
    }

    public static String getProjectQueryParam(String project) {
        return getProjectQueryParam(project, BUILD_PROJECT_PARAM);
    }

    /**
     * Creates a build info link to the published build in JFrog platform (Artifactory V7)
     *
     * @param platformUrl Base platform URL
     * @param buildName   Build name of the published build
     * @param buildNumber Build number of the published build
     * @param timeStamp   Timestamp (started date time in milliseconds) of the published build
     * @return Link to the published build in JFrog platform e.g. https://myartifactory.com/ui/builds/gradle-cli/1/1619429119501/published
     */
    public static String createBuildInfoUrl(String platformUrl, String buildName, String buildNumber, String timeStamp, String project) {
        return String.format("%s/%s/%s/%s/%s", platformUrl + BUILD_BROWSE_PLATFORM_URL, encodeUrl(buildName), encodeUrl(buildNumber), timeStamp, "published" + getProjectQueryParam(project));
    }

    /**
     * Creates a build info link to the published build in Artifactory (Artifactory V6 or below)
     *
     * @param artifactoryUrl Base Artifactory URL
     * @param buildName      Build name of the published build
     * @param buildNumber    Build number of the published build
     * @return Link to the published build in Artifactory e.g. https://myartifactory.com/artifactory/webapp/builds/gradle-cli/1
     */
    public static String createBuildInfoUrl(String artifactoryUrl, String buildName, String buildNumber) {
        return String.format("%s/%s/%s", artifactoryUrl + BUILD_BROWSE_URL, encodeUrl(buildName), encodeUrl(buildNumber));
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Could not build the build-info object.");
        throwException(response);
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (buildInfo == null) {
            return;
        }
        buildInfoJson = toJsonString(buildInfo);
    }

    @Override
    public HttpRequestBase createRequest() {
        HttpPut request = new HttpPut(BUILD_REST_URL + getProjectQueryParam(buildInfo.getProject()));
        StringEntity stringEntity = new StringEntity(buildInfoJson, "UTF-8");
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        request.setEntity(stringEntity);
        log.info("Deploying build info to: " + BUILD_REST_URL);
        return request;
    }

    @Override
    public Void execute(JFrogHttpClient client) throws IOException {
        super.execute(client);
        String url;
        if (StringUtils.isNotBlank(platformUrl)) {
            url = createBuildInfoUrl(platformUrl, buildInfo.getName(), buildInfo.getNumber(), String.valueOf(buildInfo.getStartedMillis()), buildInfo.getProject());
        } else {
            url = createBuildInfoUrl(client.getUrl(), buildInfo.getName(), buildInfo.getNumber());
        }
        log.info("Build successfully deployed. Browse it in Artifactory under " + url);
        return result;
    }
}