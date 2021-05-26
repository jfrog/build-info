package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;

public class Version extends JFrogService<ArtifactoryVersion> {
    private static final String VERSION_REST_URL = "api/system/version";
    private final Log log;

    public Version(Log logger) {
        super(logger);
        log = logger;
        result = ArtifactoryVersion.NOT_FOUND;
    }

    @Override
    public HttpRequestBase createRequest() {
        return new HttpGet(VERSION_REST_URL);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        JsonNode result = getMapper().readTree(stream);
        log.debug("Version result: " + result);
        String version = result.get("version").asText();
        JsonNode addonsNode = result.get("addons");
        boolean hasAddons = (addonsNode != null) && addonsNode.iterator().hasNext();
        this.result = new ArtifactoryVersion(version, hasAddons);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            result = ArtifactoryVersion.NOT_FOUND;
        } else {
            throwException(entity, getStatusCode());
        }
    }
}