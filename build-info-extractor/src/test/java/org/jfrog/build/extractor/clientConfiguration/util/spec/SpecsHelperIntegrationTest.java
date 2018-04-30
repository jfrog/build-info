package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
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
 * Performs tests using the resources provided by IntegrationTestsBase (such as Artifactory, localRepo, and credentials).
 */
@Test
public class SpecsHelperIntegrationTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_specs_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private SpecsHelper specsHelper = new SpecsHelper(log);

    private static final String INTEGRATION_TESTS = "/integration/tests";
    private static final String DEFAULT_SPEC_PATH = "/integration/default";
    private static final String UPLOAD_SPEC = "upload.json";
    private static final String DOWNLOAD_SPEC = "download.json";
    private static final String EXPECTED = "expected.json";

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
        List<Artifact> uploaded = specsHelper.uploadArtifactsBySpec(uploadSpec, uploadFromPath, new HashMap<String, String>(), buildInfoClient);
        Reporter.log("Uploaded " + uploaded.size() + " artifacts", true);

        // Download artifacts to compare against the expected result.
        List<Dependency> downloaded = specsHelper.downloadArtifactsBySpec(downloadSpec, dependenciesClient, tempWorkspace.getPath());
        Reporter.log("Downloaded " + downloaded.size() + " artifacts", true);

        // Verify expected results
        verifyExpected(expected);
    }

    private void verifyExpected(Expected expected) {
        // Verify tempWorkspace exists
        Assert.assertTrue(tempWorkspace.exists(), "The path: '" + tempWorkspace.getPath() + "' does not exist");
        // Verify expected results
        Collection<File> downloadedFiles = FileUtils.listFiles(tempWorkspace, null, true);
        for (String path : expected.getFiles()) {
            File f = new File(tempWorkspace, path);
            Assert.assertTrue(downloadedFiles.contains(f), "Missing file: '" + path + "'.");
            downloadedFiles.remove(f);
        }

        for (File f : downloadedFiles) {
            Assert.fail("Unexpected file: '" + f.getPath() + "'.");
        }
    }

    @DataProvider
    private Object[][] testCases() throws IOException, URISyntaxException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Get default upload, download specs.
        File defaultSpecPath = new File(this.getClass().getResource(DEFAULT_SPEC_PATH).toURI()).getCanonicalFile();
        String defaultUpload = readSpec(new File(defaultSpecPath, UPLOAD_SPEC));
        String defaultDownload = readSpec(new File(defaultSpecPath, DOWNLOAD_SPEC));

        File searchPath = new File(this.getClass().getResource(INTEGRATION_TESTS).toURI()).getCanonicalFile();
        Set<String> testPaths = new HashSet<>();
        listTestPaths(searchPath, testPaths);

        Object[][] tests = new Object[testPaths.size()][4];
        int i = 0;
        for (String testPath : testPaths) {
            String uploadSpec = defaultUpload;
            File uploadSpecFile = new File(testPath, UPLOAD_SPEC);
            if (uploadSpecFile.exists()) {
                uploadSpec = readSpec(uploadSpecFile);
            }

            String downloadSpec = defaultDownload;
            File downloadSpecFile = new File(testPath, DOWNLOAD_SPEC);
            if (downloadSpecFile.exists()) {
                downloadSpec = readSpec(downloadSpecFile);
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

    /**
     * Read spec file and replace the placeholder test data.
     *
     * @param specFile
     * @return
     * @throws IOException
     */
    private String readSpec(File specFile) throws IOException {
        String spec = FileUtils.readFileToString(specFile);
        spec = StringUtils.replace(spec, LOCAL_REPO_PLACEHOLDER, localRepo);
        spec = StringUtils.replace(spec, VIRTUAL_REPO_PLACEHOLDER, virtualRepo);
        spec = StringUtils.replace(spec, TEMP_FOLDER_PLACEHOLDER, tempWorkspace.getPath());
        return StringUtils.replace(spec, "${WORKSPACE}", tempWorkspace.getPath());
    }

    /**
     * Expected inner class for testing proposes.
     * Contains the local files expected to be found after successful download.
     */
    private static class Expected {
        private List<String> files;

        public List<String> getFiles() {
            return files;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }
    }
}