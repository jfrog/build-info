package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class DeleteProperties extends VoidJFrogService {
    public static final String DELETE_PROPERTIES_ENDPOINT = "api/storage/";

    private final String relativePath;
    private final String properties;

    public DeleteProperties(String relativePath, String properties, Log log) {
        super(log);
        this.relativePath = relativePath;
        this.properties = properties;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = DELETE_PROPERTIES_ENDPOINT + encodeUrl(relativePath + "?properties=" + properties);
        return new HttpDelete(requestUrl);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to delete properties to '" + relativePath + "'");
        throwException(entity, getStatusCode());
    }
}
