package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Integration tests for the SpecHelper.
 * Performs tests using the infrastructure resources provided by 'IntegrationTestsBase' (such as Artifactory, localRepo, and credentials),
 * and the data resources that are prepared by 'testCases'.
 */
@Test
public class SpecsHelperIntegrationTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_specs_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private SpecsHelper specsHelper = new SpecsHelper(log);

    private static final String INTEGRATION_TESTS = "/integration/tests";
    private static final String DEFAULT_SPEC_PATH = "/integration/default";

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo);
    }

    @Test(dataProvider = "testCases")
    public void integrationTests(String testName, String uploadSpec, String downloadSpec, Expected expected) throws Exception {
        Reporter.log("Running test: " + testName, true);

        // Upload artifacts.
        File uploadFromPath = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        List<Artifact> uploaded = specsHelper.uploadArtifactsBySpec(uploadSpec, uploadFromPath, new HashMap<String, String>(), buildInfoClientBuilder);
        Reporter.log("Uploaded " + uploaded.size() + " artifacts", true);

        // Download artifacts to compare against the expected result.
        List<Dependency> downloaded = specsHelper.downloadArtifactsBySpec(downloadSpec, dependenciesClient, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", true);

        // Verify expected results
        verifyExpected(expected, tempWorkspace);
    }

    /**
     * This data provider goes over all cases in "resources/integration/tests/" and creates triplets of upload and download fileSpecs,
     * and an 'expected' json file that lists all the expected downloaded files.
     * If the current case is missing a download or upload fileSpec, the corresponding default fileSpec ("resources/integration/default/") is used instead.
     * The created triplets are then provided to 'integrationTests' for testing.
     * */
    @DataProvider
    private Object[][] testCases() throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Get default upload, download specs.
        File defaultSpecPath = new File(this.getClass().getResource(DEFAULT_SPEC_PATH).toURI()).getCanonicalFile();
        String defaultUpload = readSpec(new File(defaultSpecPath, UPLOAD_SPEC), tempWorkspace.getPath());
        String defaultDownload = readSpec(new File(defaultSpecPath, DOWNLOAD_SPEC), tempWorkspace.getPath());

        File searchPath = new File(this.getClass().getResource(INTEGRATION_TESTS).toURI()).getCanonicalFile();
        Set<String> testPaths = new HashSet<>();
        listTestPaths(searchPath, testPaths);

        Object[][] tests = new Object[testPaths.size()][4];
        int i = 0;
        for (String testPath : testPaths) {
            String uploadSpec = defaultUpload;
            File uploadSpecFile = new File(testPath, UPLOAD_SPEC);
            if (uploadSpecFile.exists()) {
                uploadSpec = readSpec(uploadSpecFile, tempWorkspace.getPath());
            }

            String downloadSpec = defaultDownload;
            File downloadSpecFile = new File(testPath, DOWNLOAD_SPEC);
            if (downloadSpecFile.exists()) {
                downloadSpec = readSpec(downloadSpecFile, tempWorkspace.getPath());
            }
            try {
                Expected expected = mapper.readValue(new File(testPath, EXPECTED), Expected.class);
                tests[i] = new Object[]{testPath, uploadSpec, downloadSpec, expected};
            } catch (IOException e) {
                throw new IOException("Caught error during parsing expected results at path: " + testPath, e);
            }
            i++;
        }
        return tests;
    }

    /**
     * Add all paths containing tests to testPaths from the provided path
     *
     * @param path
     * @param testPaths
     * @throws IOException
     */
    private void listTestPaths(File path, Set<String> testPaths) throws IOException {
        if (path == null) {
            return;
        }
        File[] files = path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        if (files == null) {
            return;
        }
        // Test directory is a directory that does not contain other directories.
        if (files.length == 0) {
            testPaths.add(path.getPath());
        }
        for (File f : files) {
            listTestPaths(f, testPaths);
        }
    }
}