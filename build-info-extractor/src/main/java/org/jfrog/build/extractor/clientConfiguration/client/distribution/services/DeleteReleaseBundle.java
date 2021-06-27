package org.jfrog.build.extractor.clientConfiguration.client.distribution.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DeleteReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

/**
 * @author yahavi
 */
public class DeleteReleaseBundle extends DistributeReleaseBundle {
    public DeleteReleaseBundle(String name, String version, boolean sync, DeleteReleaseBundleRequest deleteRequest, Log log) {
        super(name, version, sync, deleteRequest, log);
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
        HttpPost request = new HttpPost(String.format("%s/%s/%s/delete", DISTRIBUTE_RELEASE_BUNDLE_ENDPOINT, name, version));
        request.setHeader("Accept", " application/json");
        StringEntity stringEntity = new StringEntity(toJsonString(super.request));
        stringEntity.setContentType("application/json");
        request.setEntity(stringEntity);
        return request;
    }

    @Override
    void logCommand() {
        log.info(request.isDryRun() ? "[Dry run] " : "" + "Deleting:" + name + " / " + version);
    }

    @Override
    void waitForDistribution(JFrogHttpClient client) throws IOException {
        String trackerId = result.getTrackerId();
        GetDistributionStatus getDistributionStatusService = new GetDistributionStatus(name, version, trackerId, log);
        for (int timeElapsed = 0; timeElapsed < DEFAULT_MAX_WAIT_MINUTES * 60; timeElapsed += DEFAULT_SYNC_SLEEP_INTERVAL) {
            if (timeElapsed % 60 == 0) {
                log.info(String.format("Sync: Deleting %s/%s...", name, version));
            }
            DistributionStatusResponse statusResponse = getDistributionStatusService.execute(client);
            if (statusResponse == null) {
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
