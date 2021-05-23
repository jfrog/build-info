package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;

public class SearchArtifactsByProperties extends JFrogService<PropertySearchResult> {
    private static final String SEARCH_ARTIFACT_BY_PROPERTIES_ENDPOINT = "api/search/prop?";

    private final String properties;

    public SearchArtifactsByProperties(String properties, Log log) {
        super(log);
        this.properties = properties;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String replacedProperties = StringUtils.replaceEach(properties, new String[]{";", "+"}, new String[]{"&", ""});
        String url = SEARCH_ARTIFACT_BY_PROPERTIES_ENDPOINT + replacedProperties;
        return new HttpGet(url);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        log.error("Failed to search artifact by the properties '" + properties + "'");
        throwException(entity, getStatusCode());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper(false).readValue(stream, PropertySearchResult.class);
    }
}
