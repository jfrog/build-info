package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.FileNotFoundException;
import java.io.IOException;

public class DeleteRepositoryContent extends VoidJFrogService {
    private final String repository;

    public DeleteRepositoryContent(String repository, Log logger) {
        super(logger);
        this.repository = repository;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpDelete(encodeUrl(repository));
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            throw new FileNotFoundException("Bad credentials");
        }
        throwException(entity, getStatusCode());
    }
}
