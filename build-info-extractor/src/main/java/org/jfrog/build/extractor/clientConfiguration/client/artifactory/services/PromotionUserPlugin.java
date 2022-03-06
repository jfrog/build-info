package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;
import java.util.Map;

import static org.jfrog.build.extractor.UrlUtils.appendParamsToUrl;
import static org.jfrog.build.extractor.UrlUtils.encodeUrlPathPart;


public class PromotionUserPlugin extends VoidJFrogService {
    public static final String PROMOTION_USER_PLUGIN_ENDPOINT = "/api/plugins/build/promote/";

    private final String promotionName;
    private final String buildName;
    private final String buildNumber;
    private final Map<String, String> requestParams;

    public PromotionUserPlugin(String promotionName, String buildName, String buildNumber, Map<String, String> requestParams, Log logger) {
        super(logger);
        this.promotionName = promotionName;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.requestParams = requestParams;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(PROMOTION_USER_PLUGIN_ENDPOINT)
                .append(promotionName).append("/")
                .append(encodeUrlPathPart(buildName)).append("/")
                .append(encodeUrlPathPart(buildNumber)).append("?");
        appendParamsToUrl(requestParams, urlBuilder);
        return new HttpPost(urlBuilder.toString());
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to promote user plugin.");
        throwException(entity, statusCode);
    }
}
