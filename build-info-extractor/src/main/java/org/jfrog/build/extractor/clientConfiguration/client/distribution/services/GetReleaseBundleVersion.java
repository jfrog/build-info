package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.GetReleaseBundleStatusResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yahavi
 */
public class GetReleaseBundleVersion extends JFrogService<GetReleaseBundleStatusResponse> {
    private static final String CREAT_RELEASE_BUNDLE_ENDPOINT = "api/v1/release_bundle";
    private final String version;
    private final String name;

    public GetReleaseBundleVersion(String name, String version, Log logger) {
        super(logger);
        this.name = name;
        this.version = version;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(createUrl());
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (StringUtils.isBlank(name)) {
            throw new IOException("Release bundle name is mandatory");
        }
        if (StringUtils.isBlank(version)) {
            throw new IOException("Release bundle version is mandatory");
        }
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, GetReleaseBundleStatusResponse.class);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            return;
        }
        super.handleUnsuccessfulResponse(entity);
    }

    private String createUrl() {
        return String.format("%s/%s/%s", CREAT_RELEASE_BUNDLE_ENDPOINT, name, version);
    }
}
