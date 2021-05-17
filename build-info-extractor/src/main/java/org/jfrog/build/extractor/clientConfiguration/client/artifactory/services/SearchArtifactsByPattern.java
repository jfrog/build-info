package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;

public class SearchArtifactsByPattern extends JFrogService<PatternResultFileSet> {
    private static final String SEARCH_ARTIFACT_BY_PATTERN_ENDPOINT = "api/search/pattern?pattern=";

    private final String pattern;

    public SearchArtifactsByPattern(String pattern, Log log) {
        super(PatternResultFileSet.class, log);
        this.pattern = pattern;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(SEARCH_ARTIFACT_BY_PATTERN_ENDPOINT + pattern);
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to search artifact by the pattern '" + pattern + "'");
        throwException(response);
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        result = getMapper(false).readValue(stream, resultClass);
    }
}
