package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.PublishBuildInfo.getProjectQueryParam;
import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;
import static org.jfrog.build.extractor.UrlUtils.encodeUrlPathPart;

public class StageBuild extends VoidJFrogService {
    private static final String BUILD_STAGING_STRATEGY_ENDPOINT = "api/build/promote/";
    private final String buildName;
    private final String buildNumber;
    private final String project;
    private final Promotion promotion;

    public StageBuild(String buildName, String buildNumber, String project, Promotion promotion, Log logger) {
        super(logger);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.project = project;
        this.promotion = promotion;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String url = String.format("%s%s/%s%s", BUILD_STAGING_STRATEGY_ENDPOINT, encodeUrlPathPart(buildName),
                encodeUrlPathPart(buildNumber), getProjectQueryParam(project));
        HttpPost request = new HttpPost(url);
        StringEntity stringEntity = new StringEntity(toJsonString(promotion));
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory.build.PromotionRequest+json");
        request.setEntity(stringEntity);
        String logMsg = String.format("Promotion build %s, #%s", buildName, buildNumber);
        if (StringUtils.isNotBlank(project)) {
            logMsg += ", in project " + project;
        }
        log.info(logMsg);
        return request;
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Promotion failed.");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for promotion.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for promotion.");
        }
    }
}
