package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
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
        super((Class<List<String>>) (Class<?>) List.class, logger);
        result = new ArrayList<>();
        this.repositoryType = repositoryType;
        log = logger;
    }

    @Override
    public HttpRequestBase createRequest() {
        String endPoint = REPOS_REST_URL;
        switch (repositoryType) {
            case LOCAL:
                endPoint += "local";
                break;
            case REMOTE:
                endPoint += "remote";
                break;
            case VIRTUAL:
                endPoint += "virtual";
        }
        HttpGet req = new HttpGet(endPoint);
        log.debug("Requesting repositories list from: " + endPoint);
        return req;
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        GetRepositoriesKeyResponse localRepositories = getMapper(true).readValue(stream, GetRepositoriesKeyResponse.class);
        result = localRepositories.getRepositoriesKey();
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to obtain list of repositories.");
        throwException(response);
    }
}