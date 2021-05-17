package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;
import java.io.InputStream;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class DistributeBuild extends VoidJFrogService {
    private static final String BUILD_REST_URL = "api/build";
    private final String buildName;
    private final String buildNumber;
    private final Distribution promotion;

    public DistributeBuild(String buildName, String buildNumber, Distribution promotion, Log log) {
        super(log);
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.promotion = promotion;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        String urlBuilder = BUILD_REST_URL + "/distribute/" +
                encodeUrl(buildName) + "/" + encodeUrl(buildNumber);
        HttpPost request = new HttpPost(urlBuilder);
        StringEntity stringEntity = new StringEntity(toJsonString(promotion));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        log.info("Distributing build " + buildName + ", #" + buildNumber);
        return request;
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        log.info(String.format("Successfully distributed build %s/%s", buildName, buildNumber));

    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (StringUtils.isBlank(buildName)) {
            throw new IllegalArgumentException("Build name is required for distribution.");
        }
        if (StringUtils.isBlank(buildNumber)) {
            throw new IllegalArgumentException("Build number is required for distribution.");
        }
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Distribution failed.");
        throwException(response);
    }
}
