package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.ci.BuildInfo;
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

    private final Build build;
    private final String platformUrl;
    private String buildJson;

    public PublishBuildInfo(BuildInfo buildInfo, String platformUrl, Log logger) {
        super(logger);
        this.build = buildInfo.ToBuild();
        this.platformUrl = platformUrl;
    }

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
     * @param encode      True if should encode build name and build number
     * @return Link to the published build in JFrog platform e.g. https://myartifactory.com/ui/builds/gradle-cli/1/1619429119501/published
     */
    public static String createBuildInfoUrl(String platformUrl, String buildName, String buildNumber, String timeStamp, String project, boolean encode) {
        if (encode) {
            buildName = encodeUrl(buildName);
            buildNumber = encodeUrl(buildNumber);
        }
        return String.format("%s/%s/%s/%s/%s", platformUrl + BUILD_BROWSE_PLATFORM_URL, buildName, buildNumber, timeStamp, "published" + getProjectQueryParam(project));
    }

    /**
     * Creates a build info link to the published build in Artifactory (Artifactory V6 or below)
     *
     * @param artifactoryUrl Base Artifactory URL
     * @param buildName      Build name of the published build
     * @param buildNumber    Build number of the published build
     * @param encode         True if should encode build name and build number
     * @return Link to the published build in Artifactory e.g. https://myartifactory.com/artifactory/webapp/builds/gradle-cli/1
     */
    public static String createBuildInfoUrl(String artifactoryUrl, String buildName, String buildNumber, boolean encode) {
        if (encode) {
            buildName = encodeUrl(buildName);
            buildNumber = encodeUrl(buildNumber);
        }
        return String.format("%s/%s/%s", artifactoryUrl + BUILD_BROWSE_URL, buildName, buildNumber);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Could not build the build-info object.");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (build == null) {
            return;
        }
        buildJson = toJsonString(build);
    }

    @Override
    public HttpRequestBase createRequest() {
        HttpPut request = new HttpPut(BUILD_REST_URL + getProjectQueryParam(build.getProject()));
        StringEntity stringEntity = new StringEntity(buildJson, "UTF-8");
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        request.setEntity(stringEntity);
        log.info("Deploying build info...");
        return request;
    }

    @Override
    public Void execute(JFrogHttpClient client) throws IOException {
        super.execute(client);
        String url;
        if (StringUtils.isNotBlank(platformUrl)) {
            url = createBuildInfoUrl(platformUrl, build.getName(), build.getNumber(), String.valueOf(build.getStartedMillis()), build.getProject(), true);
        } else {
            url = createBuildInfoUrl(client.getUrl(), build.getName(), build.getNumber(), true);
        }
        log.info("BuildInfo successfully deployed. Browse it in Artifactory under " + url);
        return result;
    }
}