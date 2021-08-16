package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.PROJECTS_ENDPOINT;

public class CreateProject extends VoidJFrogService {
    private final String projectJsonConfig;

    public CreateProject(String projectJsonConfig, Log logger) {
        super(logger);
        this.projectJsonConfig = projectJsonConfig;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(PROJECTS_ENDPOINT);
        StringEntity stringEntity = new StringEntity(projectJsonConfig, ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return request;
    }
}
