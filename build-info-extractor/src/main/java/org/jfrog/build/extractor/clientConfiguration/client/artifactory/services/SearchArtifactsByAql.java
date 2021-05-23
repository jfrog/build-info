package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
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
        super(log);
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
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to search artifact by the aql '" + aql + "'");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper(true).readValue(stream, AqlSearchResult.class);
    }
}
