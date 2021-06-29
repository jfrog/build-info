package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DeleteReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributeReleaseBundleResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

/**
 * @author yahavi
 */
public class DeleteReleaseBundle extends JFrogService<DistributeReleaseBundleResponse> {
    static final String DELETE_RELEASE_BUNDLE_ENDPOINT = "api/v1/distribution";
    static final int DEFAULT_SYNC_SLEEP_INTERVAL = 10; // 10 seconds
    static final int DEFAULT_MAX_WAIT_MINUTES = 60;    // 60 minutes

    private final DeleteReleaseBundleRequest request;
    private final String version;
    private final boolean sync;
    private final String name;

    public DeleteReleaseBundle(String name, String version, boolean sync, DeleteReleaseBundleRequest request, Log log) {
        super(log);
        this.sync = sync;
        this.name = name;
        this.version = version;
        this.request = request;
    }

    @Override
    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
        if (request == null) {
            throw new IOException("DeleteReleaseBundleRequest parameter is mandatory");
        }
        if (StringUtils.isBlank(name)) {
            throw new IOException("Release bundle name is mandatory");
        }
        if (StringUtils.isBlank(version)) {
            throw new IOException("Release bundle version is mandatory");
        }
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(String.format("%s/%s/%s/delete", DELETE_RELEASE_BUNDLE_ENDPOINT, name, version));
        request.setHeader("Accept", " application/json");
        StringEntity stringEntity = new StringEntity(toJsonString(this.request));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        return request;
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, DistributeReleaseBundleResponse.class);
        log.debug("Deletion response: " + getStatusCode());
        log.debug("Response:  " + toJsonString(result));
    }

    @Override
    public DistributeReleaseBundleResponse execute(JFrogHttpClient client) throws IOException {
        log.info(request.isDryRun() ? "[Dry run] " : "" + "Deleting " + name + " / " + version);
        super.execute(client);
        if (sync && !request.isDryRun()) {
            waitForDeletion(client);
        }
        return result;
    }

    void waitForDeletion(JFrogHttpClient client) throws IOException {
        String trackerId = result.getTrackerId();
        GetDistributionStatus statusService = new GetDistributionStatus(name, version, trackerId, log);
        for (int timeElapsed = 0; timeElapsed < DEFAULT_MAX_WAIT_MINUTES * 60; timeElapsed += DEFAULT_SYNC_SLEEP_INTERVAL) {
            if (timeElapsed % 60 == 0) {
                log.info(String.format("Sync: Deleting %s/%s...", name, version));
            }
            DistributionStatusResponse statusResponse = statusService.execute(client);
            if (statusResponse == null || statusResponse.getStatus().equalsIgnoreCase("Completed")) {
                log.info("Sync: Distribution deleted successfully");
                return;
            }
            log.debug("Sync: Received status " + statusResponse.getStatus());
            try {
                TimeUnit.SECONDS.sleep(DEFAULT_SYNC_SLEEP_INTERVAL);
            } catch (InterruptedException e) {
                throw new IOException("Fail to wait for deletion sync", e);
            }
        }
        throw new IOException("Timeout for sync deletion");
    }
}
