package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

public class DeleteRepository extends VoidJFrogService {
    private static final String API_REPOSITORIES = "api/repositories/";
    private final String repository;

    public DeleteRepository(String repository, Log logger) {
        super(logger);
        this.repository = repository;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpDelete(API_REPOSITORIES + encodeUrl(repository));
    }
}
