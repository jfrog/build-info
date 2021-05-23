package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.RepositoryType;
import org.jfrog.build.extractor.clientConfiguration.client.response.GetRepositoriesKeyResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class GetRepositoriesKeys extends JFrogService<List<String>> {
    private static final String REPOS_REST_URL = "api/repositories?type=";

    protected final Log log;
    RepositoryType repositoryType;

    @SuppressWarnings("unchecked")
    public GetRepositoriesKeys(RepositoryType repositoryType, Log logger) {
        super(logger);
        result = new ArrayList<>();
        this.repositoryType = repositoryType;
        log = logger;
    }

    @Override
    public HttpRequestBase createRequest() {
        String endpoint = REPOS_REST_URL + repositoryType.name().toLowerCase();
        HttpGet req = new HttpGet(endpoint);
        log.debug("Requesting repositories list from: " + endpoint);
        return req;
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        GetRepositoriesKeyResponse localRepositories = getMapper(true).readValue(stream, GetRepositoriesKeyResponse.class);
        result = localRepositories.getRepositoriesKey();
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to obtain list of repositories.");
        throwException(entity, getStatusCode());
    }
}