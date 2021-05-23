package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.build.extractor.clientConfiguration.util.UrlUtils.appendParamsToUrl;

public class GetStagingStrategy extends JFrogService<Map> {
    private static final String BUILD_STAGING_STRATEGY_ENDPOINT = "api/plugins/build/staging/";
    private final String strategyName;
    private final String buildName;
    private final Map<String, String> requestParams;

    public GetStagingStrategy(String strategyName, String buildName, Map<String, String> requestParams, Log log) {
        super(log);
        this.strategyName = strategyName;
        this.buildName = buildName;
        this.requestParams = requestParams;
        result = new HashMap<>();
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(BUILD_STAGING_STRATEGY_ENDPOINT)
                .append(encodeUrl(strategyName)).append("?buildName=")
                .append(encodeUrl(buildName)).append("&");
        appendParamsToUrl(requestParams, urlBuilder);
        return new HttpGet(urlBuilder.toString());
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to obtain staging strategy.");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper(true).readValue(stream, Map.class);
    }
}
