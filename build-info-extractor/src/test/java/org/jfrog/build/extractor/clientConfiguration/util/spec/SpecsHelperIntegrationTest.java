package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.ci.Artifact;
import org.jfrog.build.api.ci.Dependency;
import org.testng.Reporter;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Integration tests for the SpecHelper.
 * Performs tests using the infrastructure resources provided by 'IntegrationTestsBase' (such as Artifactory, localRepo, and credentials),
 * and the data resources that are prepared by 'testCases'.
 */
@Test
public class SpecsHelperIntegrationTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_specs_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private final SpecsHelper specsHelper = new SpecsHelper(log);

    private static final String INTEGRATION_TESTS = "/integration/tests";
    private static final String DEFAULT_SPEC_PATH = "/integration/default";

    @BeforeClass
    public void init() throws IOException {
        super.init();
        if (!artifactoryManager.getVersion().isOSS()) {
            createTestRepo(localRepo2);
        }
    }

    @AfterClass
    protected void terminate() throws IOException {
        if (!artifactoryManager.getVersion().isOSS()) {
            deleteTestRepo(localRepo2);
        }
        super.terminate();
    }

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo1);
        deleteContentFromRepo(localRepo2);
    }

    @Test(dataProvider = "testCases")
    public void integrationTests(SingleSpecTest specTest) throws Exception {
        Reporter.log("Running test: " + specTest.testPath, false);

        // Upload artifacts.
        File uploadFromPath = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        List<Artifact> uploaded = specsHelper.uploadArtifactsBySpec(specTest.uploadSpec, uploadFromPath, new HashMap<>(), artifactoryManagerBuilder);
        Reporter.log("Uploaded " + uploaded.size() + " artifacts", false);

        // Download artifacts to compare against the expected result.
        List<Dependency> downloaded = specsHelper.downloadArtifactsBySpec(specTest.downloadSpec, artifactoryManager, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", false);

        // Verify expected results
        verifyExpected(specTest.expected, tempWorkspace);
    }

    /**
     * This data provider goes over all cases in "resources/integration/tests/" and creates triplets of upload and download fileSpecs,
     * and an 'expected' json file that lists all the expected downloaded files.
     * If the current case is missing a download or upload fileSpec, the corresponding default fileSpec ("resources/integration/default/") is used instead.
     * The created triplets are then provided to 'integrationTests' for testing.
     */
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

        SingleSpecTest[][] tests = new SingleSpecTest[testPaths.size()][4];
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
                tests[i] = new SingleSpecTest[]{new SingleSpecTest(testPath, uploadSpec, downloadSpec, expected)};
            } catch (IOException e) {
                throw new IOException("Caught error during parsing expected results at path: " + testPath, e);
            }
            i++;
        }
        return tests;
    }

    /**
     * Add all paths containing tests to testPaths from the provided path.
     *
     * @param path      - The search path
     * @param testPaths - The results
     */
    private void listTestPaths(File path, Set<String> testPaths) {
        if (path == null) {
            return;
        }
        File[] files = path.listFiles(File::isDirectory);

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

    /**
     * This class represents a single SpecsHelper integration test
     */
    private static class SingleSpecTest {
        private final String testPath;
        private final String uploadSpec;
        private final String downloadSpec;
        private final Expected expected;

        private SingleSpecTest(String testPath, String uploadSpec, String downloadSpec, Expected expected) {
            this.testPath = testPath;
            this.uploadSpec = uploadSpec;
            this.downloadSpec = downloadSpec;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return testPath;
        }
    }
}