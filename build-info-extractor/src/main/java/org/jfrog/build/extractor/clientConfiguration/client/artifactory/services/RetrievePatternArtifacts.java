package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RetrievePatternArtifacts extends JFrogService<List<BuildPatternArtifacts>> {
    private static final String PATTERN_ARTIFACT_REST_URL = "/api/build/patternArtifacts";
    private final List<BuildPatternArtifactsRequest> requests;

    @SuppressWarnings("unchecked")
    public RetrievePatternArtifacts(List<BuildPatternArtifactsRequest> requests, Log logger) {
        super(logger);
        this.requests = requests;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        log.info("Retrieving build artifacts report from: " + PATTERN_ARTIFACT_REST_URL);
        HttpPost req = new HttpPost(PATTERN_ARTIFACT_REST_URL);
        String json = new JsonSerializer<List<BuildPatternArtifactsRequest>>().toJSON(requests);
        StringEntity stringEntity = new StringEntity(json);
        stringEntity.setContentType("application/vnd.org.jfrog.artifactory+json");
        req.setEntity(stringEntity);
        return req;
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, TypeFactory.defaultInstance().constructCollectionLikeType(List.class, BuildPatternArtifacts.class));
    }
}
