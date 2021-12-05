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

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class StageBuild extends VoidJFrogService {
    private static final String BUILD_STAGING_STRATEGY_ENDPOINT = "api/build/promote/";
    private final String buildName;
    private final String buildNumber;
    private final Promotion promotion;

    public StageBuild(String buildName, String buildNumber, Promotion promotion, Log logger) {
        super(logger);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.promotion = promotion;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(BUILD_STAGING_STRATEGY_ENDPOINT + encodeUrl(buildName) + "/" + encodeUrl(buildNumber));
        StringEntity stringEntity = new StringEntity(toJsonString(promotion));
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory.build.PromotionRequest+json");
        request.setEntity(stringEntity);
        log.info("Promotion build " + buildName + ", #" + buildNumber);
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
