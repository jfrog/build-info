package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author yahavi
 */
public class GetDistributionStatus extends JFrogService<DistributionStatusResponse> {
    private static final String GET_STATUS_ENDPOINT = "api/v1/release_bundle";
    private final String trackerId;
    private final String version;
    private final String name;

    public GetDistributionStatus(String name, String version, String trackerId, Log logger) {
        super(logger);
        this.name = name;
        this.version = version;
        this.trackerId = trackerId;
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
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(createUrl());
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, DistributionStatusResponse.class);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            result = null;
            return;
        }
        super.handleUnsuccessfulResponse(entity);
    }

    private String createUrl() {
        String url = GET_STATUS_ENDPOINT;
        if (StringUtils.isEmpty(name)) {
            return url + "/distribution";
        }
        url += "/" + name;
        if (StringUtils.isEmpty(version)) {
            return url + "/distribution";
        }
        url += "/" + version + "/distribution";
        if (StringUtils.isNotEmpty(trackerId)) {
            return url + "/" + trackerId;
        }
        return url;
    }
}
