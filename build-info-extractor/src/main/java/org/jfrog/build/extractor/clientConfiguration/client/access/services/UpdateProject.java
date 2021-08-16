package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class UpdateProject extends VoidJFrogService {
    private static final String CREATE_PROJECT_ENDPOINT = "api/v1/projects";
    private final String projectKey;
    private final String projectJsonConfig;

    public UpdateProject(String projectKey, String projectJsonConfig, Log logger) {
        super(logger);
        this.projectKey = projectKey;
        this.projectJsonConfig = projectJsonConfig;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPut request = new HttpPut(CREATE_PROJECT_ENDPOINT + "/" + encodeUrl(projectKey));
        request.addHeader("Accept", "application/json");
        StringEntity stringEntity = new StringEntity(projectJsonConfig, ContentType.create("application/json"));
        request.setEntity(stringEntity);
        return request;
    }
}
