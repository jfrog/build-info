package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.repository.RepositoryConfig;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.RepositoryType;

import java.io.IOException;
import java.io.InputStream;

public class CheckRepositoryType extends JFrogService<Boolean> {
    private static final String REPOS_REST_URL = "api/repositories/";
    private final RepositoryType repositoryType;
    private final String repositoryKey;

    public CheckRepositoryType(RepositoryType repositoryType, String repositoryKey, Log log) {
        super(Boolean.class, log);
        this.repositoryType = repositoryType;
        this.repositoryKey = repositoryKey;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(REPOS_REST_URL + repositoryKey);
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        RepositoryConfig repositoryConfig = getMapper(true).readValue(stream, RepositoryConfig.class);
        switch (repositoryType) {
            case LOCAL:
                result = "local".equals(repositoryConfig.getRclass());
                break;
            case REMOTE:
                result = "remote".equals(repositoryConfig.getRclass());
        }
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to retrieve repository configuration '" + repositoryKey + "'");
        throwException(response);
    }
}
