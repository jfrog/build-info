package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.client.DownloadResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.CreateReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DeleteReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DistributeReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributeReleaseBundleResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.DistributionRules;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.FilesGroup;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;
import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class RemoteDistributionManagerTest extends DistributionManagerTest {

    @AfterMethod
    protected void cleanup() throws IOException {
        try {
            distributionManager.deleteReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, false, createDeletionRequest());
        } catch (IOException ignored) {
            // Ignore
        }
        super.cleanup();
    }

    @Test
    public void distributeWithMappingTest() throws IOException {
        // Create a release bundle with path mapping
        String fileName = uploadFile();
        FileSpec fileSpec = new FileSpec();
        FilesGroup filesGroup = new FilesGroup().setPattern(localRepo1 + "/data/(*)").setTarget(localRepo2 + "/data2/{1}");
        fileSpec.addFilesGroup(filesGroup);
        CreateReleaseBundleRequest request = new CreateReleaseBundleRequest.Builder(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION)
                .spec(fileSpec)
                .signImmediately(true)
                .build();
        distributionManager.createReleaseBundle(request);

        // Distribute the release bundle
        distributionManager.distributeReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, true, createDistributionRequest());

        // Download the file from the new path
        DownloadResponse downloadResponse = artifactoryManager.download(localRepo2 + "/data2/" + fileName);
        assertTrue(StringUtils.isNotBlank(downloadResponse.getContent()));

        // Delete release bundle
        distributionManager.deleteReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, true, createDeletionRequest());
    }

    @Test
    public void asyncDistributionTest() throws IOException, InterruptedException {
        // Create and sign a release bundle
        distributionManager.createReleaseBundle(createRequestBuilder().build());
        distributionManager.signReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, "");

        // Distribute release bundle
        DistributeReleaseBundleResponse response = distributionManager.distributeReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, false, createDistributionRequest());
        assertNotNull(response);
        String trackerId = response.getTrackerId();
        assertTrue(StringUtils.isNotBlank(trackerId));

        // Wait for distribution
        boolean success = false;
        for (int i = 0; i < 120; i++) {
            DistributionStatusResponse status = distributionManager.getDistributionStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, trackerId);
            if ("Failed".equalsIgnoreCase(status.getStatus())) {
                fail("Distribution of " + RELEASE_BUNDLE_NAME + "/" + RELEASE_BUNDLE_VERSION + " failed: " + toJsonString(status));
            }
            if ("Completed".equalsIgnoreCase(status.getStatus())) {
                success = true;
                break;
            }
            log.info("Waiting for " + RELEASE_BUNDLE_NAME + "/" + RELEASE_BUNDLE_VERSION + "...");
            TimeUnit.SECONDS.sleep(1);
        }
        assertTrue(success, "Distribution of " + RELEASE_BUNDLE_NAME + "/" + RELEASE_BUNDLE_VERSION + " failed");

        // Delete distribution and wait for deletion
        DistributeReleaseBundleResponse deleteResponse = distributionManager.deleteReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, false, createDeletionRequest());
        assertNotNull(deleteResponse);
        success = false;
        for (int i = 0; i < 120; i++) {
            DistributionStatusResponse status = distributionManager.getDistributionStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, trackerId);
            if (status == null) {
                success = true;
                break;
            }
            log.info("Waiting for deletion of " + RELEASE_BUNDLE_NAME + "/" + RELEASE_BUNDLE_VERSION + "...");
            TimeUnit.SECONDS.sleep(1);
        }
        assertTrue(success, "Deletion of " + RELEASE_BUNDLE_NAME + "/" + RELEASE_BUNDLE_VERSION + " failed");
    }

    private DistributeReleaseBundleRequest createDistributionRequest() {
        DistributeReleaseBundleRequest distributionRequest = new DistributeReleaseBundleRequest();
        List<DistributionRules> distributionRules = Lists.newArrayList(new DistributionRules.Builder().siteName("*").build());
        distributionRequest.setDistributionRules(distributionRules);
        return distributionRequest;
    }

    private DeleteReleaseBundleRequest createDeletionRequest() {
        DeleteReleaseBundleRequest deletionRequest = new DeleteReleaseBundleRequest();
        List<DistributionRules> distributionRules = Lists.newArrayList(new DistributionRules.Builder().siteName("*").build());
        deletionRequest.setDistributionRules(distributionRules);
        deletionRequest.setOnSuccess(DeleteReleaseBundleRequest.OnSuccess.delete);
        return deletionRequest;
    }
}
