package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class DeleteProperties extends VoidJFrogService {
    private final String url;
    private final String properties;

    public DeleteProperties(String url, String properties, Log log) {
        super(log);
        this.url = url;
        this.properties = properties;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String requestUrl = encodeUrl(url + "?properties=" + properties);
        return new HttpDelete(requestUrl);
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to delete properties to '" + url + "'");
        throwException(response);
    }
}
