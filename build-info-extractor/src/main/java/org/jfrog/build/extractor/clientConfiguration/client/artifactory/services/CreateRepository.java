package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class CreateRepository extends VoidJFrogService {
    private static final String CREATE_REPOSITORY_ENDPOINT = "api/repositories/";
    private final String repositoryKey;
    private final String repositoryJsonConfig;

    public CreateRepository(String repositoryKey, String repositoryJsonConfig, Log logger) {
        super(logger);
        this.repositoryKey = repositoryKey;
        this.repositoryJsonConfig = repositoryJsonConfig;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPut request = new HttpPut(CREATE_REPOSITORY_ENDPOINT + repositoryKey);
        StringEntity stringEntity = new StringEntity(repositoryJsonConfig, ContentType.create("application/json"));
        request.setEntity(stringEntity);
        return request;
    }
}
