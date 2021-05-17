package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class UpdateFileProperty extends VoidJFrogService {
    private static final String UPDATE_FILE_PROPERTY_ENDPOINT = "api/storage/";
    private final String itemPath;
    private final String properties;

    public UpdateFileProperty(String itemPath, String properties, Log logger) {
        super(logger);
        this.itemPath = itemPath;
        this.properties = properties;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpPut(UPDATE_FILE_PROPERTY_ENDPOINT + encodeUrl(itemPath) + "?" + "properties=" + encodeUrl(properties));
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed while trying to set properties on docker layer.");
        throwException(response);
    }
}
