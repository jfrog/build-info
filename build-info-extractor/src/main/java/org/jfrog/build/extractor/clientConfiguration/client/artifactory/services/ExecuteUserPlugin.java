package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;
import java.util.Map;

import static org.jfrog.build.extractor.UrlUtils.appendParamsToUrl;

public class ExecuteUserPlugin extends VoidJFrogService {
    public static final String EXECUTE_USER_PLUGIN_ENDPOINT = "api/storage/";

    private final String executionName;
    private final Map<String, String> requestParams;

    public ExecuteUserPlugin(String executionName, Map<String, String> requestParams, Log logger) {
        super(logger);
        this.executionName = executionName;
        this.requestParams = requestParams;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(EXECUTE_USER_PLUGIN_ENDPOINT).append(executionName).append("?");
        appendParamsToUrl(requestParams, urlBuilder);
        return new HttpPost(urlBuilder.toString());
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to execute user plugin.");
        throwException(entity, getStatusCode());
    }
}
