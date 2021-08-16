package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.PROJECTS_ENDPOINT;

public class GetProject extends JFrogService<String> {
    private final String projectKey;

    public GetProject(String projectKey, Log logger) {
        super(logger);
        this.projectKey = projectKey;
        result = "";
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(PROJECTS_ENDPOINT + "/" + encodeUrl(projectKey));
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed get project with key: '" + projectKey + "'.");
        throwException(entity, getStatusCode());
    }
}
