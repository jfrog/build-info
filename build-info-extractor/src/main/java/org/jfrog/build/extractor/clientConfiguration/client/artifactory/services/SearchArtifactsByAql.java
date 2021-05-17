package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;

public class SearchArtifactsByAql extends JFrogService<AqlSearchResult> {
    private static final String SEARCH_ARTIFACT_BY_AQL_ENDPOINT = "api/search/aql";

    private final String aql;

    public SearchArtifactsByAql(String aql, Log log) {
        super(AqlSearchResult.class, log);
        this.aql = aql;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(SEARCH_ARTIFACT_BY_AQL_ENDPOINT);
        StringEntity entity = new StringEntity(aql);
        request.setEntity(entity);
        return request;
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to search artifact by the aql '" + aql + "'");
        throwException(response);
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        result = getMapper(true).readValue(stream, resultClass);
    }
}
