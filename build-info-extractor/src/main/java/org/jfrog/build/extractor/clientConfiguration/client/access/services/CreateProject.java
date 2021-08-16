package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class CreateProject extends VoidJFrogService {
    private static final String CREATE_PROJECT_ENDPOINT = "api/v1/projects";
    private final String projectJsonConfig;

    public CreateProject(String projectJsonConfig, Log logger) {
        super(logger);
        this.projectJsonConfig = projectJsonConfig;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(CREATE_PROJECT_ENDPOINT);
        request.addHeader("Accept", "application/json");
        StringEntity stringEntity = new StringEntity(projectJsonConfig, ContentType.create("application/json"));
        request.setEntity(stringEntity);
        return request;
    }
}
