package org.jfrog.build.extractor.clientConfiguration.util;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Integration tests for various props operations.
 */
@Test
public class PropertiesTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_specs_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private SpecsHelper specsHelper = new SpecsHelper(log);

    private static final String PROPS_TEST_PATH = "/propsTests";

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo);
    }

    @Test
    public void propsTest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        File propsTestPath = new File(this.getClass().getResource(PROPS_TEST_PATH).toURI()).getCanonicalFile();

        String uploadSpec = readSpec(new File(propsTestPath, UPLOAD_SPEC), tempWorkspace.getPath());
        String propsSpec = readSpec(new File(propsTestPath, "setProps.json"), tempWorkspace.getPath());
        String downloadSpec = readSpec(new File(propsTestPath, DOWNLOAD_SPEC), tempWorkspace.getPath());
        org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelperIntegrationTest.Expected expected = mapper.readValue(new File(propsTestPath, EXPECTED), org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelperIntegrationTest.Expected.class);

        // Upload artifacts.
        File uploadFromPath = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        List<Artifact> uploaded = specsHelper.uploadArtifactsBySpec(uploadSpec, uploadFromPath, new HashMap<String, String>(), buildInfoClientBuilder);
        Reporter.log("Uploaded " + uploaded.size() + " artifacts", true);

        // Set Properties on the uploaded artifacts
        specsHelper.editPropertiesBySpec(propsSpec, dependenciesClient, EditPropertiesHelper.EditPropertiesActionType.SET, "p=v+1+d!");

        // Download artifacts to compare against the expected result.
        List<Dependency> downloaded = specsHelper.downloadArtifactsBySpec(downloadSpec, dependenciesClient, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", true);

        // Verify expected results
        verifyExpected(expected, tempWorkspace);

        // Delete Properties from the uploaded artifacts
        specsHelper.editPropertiesBySpec(propsSpec, dependenciesClient, EditPropertiesHelper.EditPropertiesActionType.DELETE, "p");

        // Clean all files from download's target
        FileUtils.deleteDirectory(tempWorkspace);

        // Download artifacts to compare against the expected result.
        downloaded = specsHelper.downloadArtifactsBySpec(downloadSpec, dependenciesClient, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", true);

        // No artifacts should be downloaded, assert that target dir was not created
        Assert.assertFalse(tempWorkspace.exists(), "The path: '" + tempWorkspace.getPath() + "' should not been created.");

    }
}

