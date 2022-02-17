package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.PROJECTS_ENDPOINT;
import static org.jfrog.build.extractor.UrlUtils.encodeUrlPathPart;

public class UpdateProject extends VoidJFrogService {
    private final String projectKey;
    private final String projectJsonConfig;

    public UpdateProject(String projectKey, String projectJsonConfig, Log logger) {
        super(logger);
        this.projectKey = projectKey;
        this.projectJsonConfig = projectJsonConfig;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPut request = new HttpPut(PROJECTS_ENDPOINT + "/" + encodeUrlPathPart(projectKey));
        StringEntity stringEntity = new StringEntity(projectJsonConfig, ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return request;
    }
}
