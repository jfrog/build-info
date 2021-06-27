package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yahavi
 */
public class Version extends JFrogService<org.jfrog.build.client.Version> {
    private static final String SYSTEM_INFO_REST_URL = "api/v1/system/info";

    public Version(Log logger) {
        super(logger);
    }

    @Override
    public HttpRequestBase createRequest() {
        return new HttpGet(SYSTEM_INFO_REST_URL);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        JsonNode result = getMapper().readTree(stream);
        log.debug("System Info result: " + result);
        String version = result.get("version").asText();
        this.result = new org.jfrog.build.client.Version(version);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            result = org.jfrog.build.client.Version.NOT_FOUND;
        } else {
            throwException(entity, getStatusCode());
        }
    }
}