package org.jfrog.build.extractor.clientConfiguration.client;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.CreateReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.request.DistributeReleaseBundleRequest;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributeReleaseBundleResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.DistributionStatusResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.response.GetReleaseBundleStatusResponse;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.DistributionRules;
import org.jfrog.build.extractor.clientConfiguration.client.distribution.types.ReleaseNotes;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.FilesGroup;
import org.jfrog.filespecs.properties.Property;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.collections.CollectionUtils;
import org.testng.collections.Lists;

import java.io.IOException;
import java.util.List;

import static org.testng.Assert.*;

/**
 * @author yahavi
 **/
public class LocalDistributionManagerTest extends DistributionManagerTest {

    @AfterMethod
    protected void cleanup() throws IOException {
        try {
            distributionManager.deleteLocalReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        } catch (IOException ignored) {
            // Ignore
        }
        super.cleanup();
    }

    @Test
    public void dryRunTest() throws IOException {
        // Create release bundle
        distributionManager.createReleaseBundle(createRequestBuilder().dryRun(true).build());

        // Assert release bundle doesn't created
        GetReleaseBundleStatusResponse bundleInfo = distributionManager.getReleaseBundleStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        assertNull(bundleInfo);

        // Create and sign release bundle
        distributionManager.createReleaseBundle(createRequestBuilder().signImmediately(true).build());

        // Distribute with dry run
        DistributeReleaseBundleRequest request = new DistributeReleaseBundleRequest();
        List<DistributionRules> distributionRules = Lists.newArrayList(new DistributionRules.Builder().siteName("*").build());
        request.setDistributionRules(distributionRules);
        request.setDryRun(true);
        DistributeReleaseBundleResponse response = distributionManager.distributeReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, true, request);

        // Assert no tracker ID returned and that the target site exist
        assertTrue(StringUtils.isBlank(response.getTrackerId()));
        List<DistributionStatusResponse.TargetArtifactory> sites = response.getSites();
        assertTrue(CollectionUtils.hasElements(sites));
    }

    @Test
    public void createUpdateDeleteReleaseBundleTest() throws IOException {
        // Create release bundle
        distributionManager.createReleaseBundle(createRequestBuilder().build(), "");

        // Assert create
        GetReleaseBundleStatusResponse bundleInfo = distributionManager.getReleaseBundleStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        assertEquals(bundleInfo.getDescription(), "Create");
        assertEquals(bundleInfo.getReleaseNotes().getContent(), "Create content");
        assertEquals(bundleInfo.getReleaseNotes().getSyntax(), ReleaseNotes.Syntax.plain_text);

        // Update release bundle
        distributionManager.updateReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION, updateRequestBuilder().build());

        // Assert update
        bundleInfo = distributionManager.getReleaseBundleStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        assertEquals(bundleInfo.getDescription(), "Update");
        assertEquals(bundleInfo.getReleaseNotes().getContent(), "Update content");

        // Delete release bundle
        distributionManager.deleteLocalReleaseBundle(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);

        // Assert deletion
        bundleInfo = distributionManager.getReleaseBundleStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        assertNull(bundleInfo);
    }

    @Test
    public void addedPropsTest() throws IOException {
        // Create a release bundle with added props
        String fileName = uploadFile();
        FileSpec fileSpec = new FileSpec();
        FilesGroup filesGroup = new FilesGroup().setTargetProps("key1=value1,value2").setPattern(localRepo1 + "/data/" + fileName);
        fileSpec.addFilesGroup(filesGroup);
        CreateReleaseBundleRequest request = new CreateReleaseBundleRequest.Builder(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION)
                .spec(fileSpec)
                .build();
        distributionManager.createReleaseBundle(request);

        // Assert added props
        GetReleaseBundleStatusResponse bundleInfo = distributionManager.getReleaseBundleStatus(RELEASE_BUNDLE_NAME, RELEASE_BUNDLE_VERSION);
        assertNotNull(bundleInfo);
        List<Property> addedProps = bundleInfo.getSpec().getQueries().get(0).getAddedProps();
        assertFalse(addedProps.isEmpty());
        Property property = addedProps.get(0);
        assertEquals(property.getKey(), "key1");
        assertEquals(property.getValues(), Sets.newHashSet("value1", "value2"));
    }
}
