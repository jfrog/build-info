package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class GetNpmAuth extends JFrogService<Properties> {
    private static final String GET_NPM_AUTH_ENDPOINT = "api/npm/auth";


    public GetNpmAuth(Log log) {
        super(log);
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(GET_NPM_AUTH_ENDPOINT);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("npm Auth request failed");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = new Properties();
        result.load(stream);
    }
}
