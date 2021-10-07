package org.jfrog.build.extractor.clientConfiguration.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.ci.Artifact;
import org.jfrog.build.api.ci.Dependency;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Integration tests for various props operations.
 */
@Test
public class PropertiesTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_specs_test_space";
    private String downloadSpec;
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private File propsTestPath;
    private final SpecsHelper specsHelper = new SpecsHelper(log);
    private ObjectMapper mapper;

    private static final String PROPS_TEST_PATH = "/propsTests";

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo1);
    }

    @Test
    public void propsTest() throws Exception {
        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        propsTestPath = new File(this.getClass().getResource(PROPS_TEST_PATH).toURI()).getCanonicalFile();
        downloadSpec = readSpec(new File(propsTestPath, DOWNLOAD_SPEC), tempWorkspace.getPath());

        // Upload artifacts.
        String uploadSpec = readSpec(new File(propsTestPath, UPLOAD_SPEC), tempWorkspace.getPath());
        File uploadFromPath = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        List<Artifact> uploaded = specsHelper.uploadArtifactsBySpec(uploadSpec, uploadFromPath, new HashMap<>(), artifactoryManagerBuilder);
        Reporter.log("Uploaded " + uploaded.size() + " artifacts", true);

        // Edit properties on the uploaded artifacts and verify.
        editPropsAndVerify("setProps.json", EditPropertiesHelper.EditPropertiesActionType.SET, "p=v+1+d!", "setExpected.json");
        editPropsAndVerify("deleteProps.json", EditPropertiesHelper.EditPropertiesActionType.DELETE, "p", "deleteExpected.json");
    }

    private void editPropsAndVerify(String specPath, EditPropertiesHelper.EditPropertiesActionType editType, String props, String expectedPath) throws IOException {
        String spec = readSpec(new File(propsTestPath, specPath), tempWorkspace.getPath());
        Expected expected = mapper.readValue(new File(propsTestPath, expectedPath), Expected.class);

        // Edit Properties on the uploaded artifacts
        specsHelper.editPropertiesBySpec(spec, artifactoryManager, editType, props);

        // DownloadBase artifacts to compare against the expected result.
        List<Dependency> downloaded = specsHelper.downloadArtifactsBySpec(downloadSpec, artifactoryManager, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", true);

        // Verify expected results
        verifyExpected(expected, tempWorkspace);

        // Clean all files from download's target
        FileUtils.deleteDirectory(tempWorkspace);
    }
}

