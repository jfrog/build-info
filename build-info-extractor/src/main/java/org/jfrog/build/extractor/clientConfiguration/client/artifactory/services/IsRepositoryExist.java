package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.InputStream;

public class IsRepositoryExist extends JFrogService<Boolean> {
    private static final String REPOS_REST_URL = "api/repositories/";

    private final String repo;

    public IsRepositoryExist(String repo, Log logger) {
        super(logger);
        this.repo = repo;
    }

    @Override
    public HttpRequestBase createRequest() {
        return new HttpGet(REPOS_REST_URL + repo);
    }

    @Override
    protected void setResponse(InputStream stream) {
        result = true;
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) {
        result = statusCode != HttpStatus.SC_BAD_REQUEST;
    }
}