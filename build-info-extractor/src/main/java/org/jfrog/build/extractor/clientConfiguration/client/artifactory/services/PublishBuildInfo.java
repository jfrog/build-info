package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

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

    Build buildInfo;
    private String buildInfoJson;

    public PublishBuildInfo(String buildInfoJson, Log logger) {
        super(logger);
        this.buildInfoJson = buildInfoJson;
    }

    public PublishBuildInfo(Build buildInfo, Log logger) {
        super(logger);
        this.buildInfo = buildInfo;
    }

    @Override
    public HttpRequestBase createRequest() {
        HttpPut request = new HttpPut(BUILD_REST_URL);
        StringEntity stringEntity = new StringEntity(buildInfoJson, "UTF-8");
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        request.setEntity(stringEntity);
        log.info("Deploying build descriptor to path: " + BUILD_REST_URL);
        return request;
    }

    @Override
    public Void execute(JFrogHttpClient client) throws IOException {
        super.execute(client);
        String url = client.getUrl() +
                BUILD_BROWSE_URL + "/" + encodeUrl(buildInfo.getName()) + "/" + encodeUrl(buildInfo.getNumber());
        log.info("Build successfully deployed. Browse it in Artifactory under " + url);
        return result;
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
}