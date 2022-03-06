package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createBuildInfoUrl;
import static org.jfrog.build.extractor.UrlUtils.getProjectQueryParam;
import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class PublishBuildInfo extends VoidJFrogService {
    private static final String BUILD_REST_URL = "/api/build";

    private final Build build;
    private final String platformUrl;
    private String buildJson;

    public PublishBuildInfo(BuildInfo buildInfo, String platformUrl, Log logger) {
        super(logger);
        this.build = buildInfo.ToBuild();
        this.platformUrl = platformUrl;
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
        boolean isPlatformUrl = isNotBlank(platformUrl);
        String url = isPlatformUrl ? platformUrl : client.getUrl();
        String buildInfoUrl = createBuildInfoUrl(url, build.getName(), build.getNumber(),
                String.valueOf(build.getStartedMillis()), build.getProject(), true, isPlatformUrl);
        if (isNotBlank(buildInfoUrl)) {
            log.info("Build-info successfully deployed. Browse it in Artifactory under " + buildInfoUrl);
        } else {
            log.debug("Couldn't create the build-info URL from Artifactory URL: " + client.getUrl());
            log.info("Build-info successfully deployed.");
        }
        return result;
    }
}