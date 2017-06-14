package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.extractor.clientConfiguration.util.AqlDependenciesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderImpl;
import org.testng.ITestContext;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.jfrog.build.extractor.clientConfiguration.util.PathsUtils.escapeSpecialChars;

/**
 * Integration tests for the SpecHelper.
 * Performs tests using the provided by IntegrationTestsBase resources (such as Artifactory, repo, and credentials).
 * The tests will clean the environment except in case of failure. In case of failure the Artifacts will remain to make investigation simpler.
 */
@Test
public class SpecsHelperIntegrationTest extends IntegrationTestsBase {

    private static final String TEST_SPACE = "bi_test_space";
    private static final String SEPARATOR = File.separator;
    private static final String TEST_WORKSPACE = System.getProperty("java.io.tmpdir") + SEPARATOR + TEST_SPACE;
    private SpecsHelper specsHelper = new SpecsHelper(log);


    /**
     * Performing a massive upload to multiple different patterns with multiple flags combinations.
     * Every spec in "uploadTest-uploadSpec-wildcard.json" and "uploadTest-uploadSpec-regexp.json" represents a test case and should have a running serial number.
     * The test numeration is done to make investigation simpler.
     * The test uploads the files by the provided upload specs and then downloads all the content of the TEST_SPACE folder to a temp directory.
     * In the end it compares the expected paths with the created in the temp directory paths .
     */
    public void testUploadSpec() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        String innerDir = "upload-test";
        File artifactsDir = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();

        // Class.getResource() returns windows path with slash prefix which brakes the upload. therefor the file creation and then the get path
        String workspace = new File(this.getClass().getResource("/workspace").getPath()).getCanonicalFile().getPath();


        // Execute simple wildcard upload
        String uploadSpec = injectVariables(innerDir, "/specs/integrationTestSpecs/uploadTest-uploadSpec-wildcard.json", workspace, false);
        List<Artifact> uploadedArtifacts = specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsDir, null, getBuildInfoClient());

        // Execute simple regexp upload
        uploadSpec = injectVariables(innerDir, "/specs/integrationTestSpecs/uploadTest-uploadSpec-regexp.json", workspace, true);
        uploadedArtifacts.addAll(specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsDir, null, getBuildInfoClient()));

        // Download the uploaded files
        String downloadDestinationDir = System.getProperty("java.io.tmpdir");
        String downloadSpec = injectVariables(innerDir, "/specs/integrationTestSpecs/uploadTest-downloadSpec.json", "", false);
        List<Dependency> downloadedArtifacts =
                specsHelper.downloadArtifactsBySpec(downloadSpec, getDependenciesClient(), downloadDestinationDir);

        // Asserting returned results length
        assert downloadedArtifacts.size() == uploadedArtifacts.size() :
                String.format("Number of downloaded and uploaded by the test artifacts should be equal, " +
                        "but uploaded - %d artifacts and downloaded - %d",
                        uploadedArtifacts.size(), downloadedArtifacts.size());

        // Assert all expected files are exists
        File downloadedFilesDir = new File(TEST_WORKSPACE + SEPARATOR + innerDir + SEPARATOR).getCanonicalFile();
        Collection<File> downloadedFiles = FileUtils.listFiles(downloadedFilesDir, null, true);
        for (String path : ConstData.UPLOAD_RESULTS) {
            assert downloadedFiles.contains(
                    new File(TEST_WORKSPACE + SEPARATOR + innerDir + SEPARATOR + path).getCanonicalFile()) :
                    String.format("Missing file! \nFile %s expected to be downloaded but it is not!",
                            TEST_WORKSPACE + SEPARATOR + innerDir + SEPARATOR + path);
        }

        // Assert no other files where downloaded
        assert ConstData.UPLOAD_RESULTS.length == downloadedFiles.size() :
                String.format("Number of downloaded files from the upload test and the expected results should be equal, " +
                                "but expected - %d artifacts and downloaded - %d",
                        ConstData.UPLOAD_RESULTS.length, downloadedFiles.size());
    }

    /**
     * Testing upload spec that contains parenthesis in the path or in the pattern.
     * Only regexp upload supports parenthesis in the pattern (as part of the path).
     */
    public void testParenthesisUploadSpec() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        String parenthesisDirName = "dir(with)parenthesis";
        String testDirName = "upload-parenthesis-test";
        String innerDir = testDirName + SEPARATOR + parenthesisDirName;
        File artifactsSourceDir = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        File artifactsUploadDir = new File(System.getProperty("java.io.tmpdir") + SEPARATOR + TEST_SPACE + SEPARATOR + innerDir).getCanonicalFile();
        String workspace = artifactsUploadDir.getCanonicalPath();

        FileUtils.copyDirectory(artifactsSourceDir, artifactsUploadDir);
        // Execute the wildcard upload
        String uploadSpec = injectVariables(innerDir,
                "/specs/integrationTestSpecs/uploadTest-uploadSpec-wildcard-parenthesis.json", workspace, false);
        List<Artifact> uploadedArtifacts = specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsUploadDir, null, getBuildInfoClient());

        // Execute the regexp upload
        uploadSpec = injectVariables(innerDir,
                "/specs/integrationTestSpecs/uploadTest-uploadSpec-regexp-parenthesis.json", workspace, true);
        uploadedArtifacts.addAll(specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsUploadDir, null, getBuildInfoClient()));


        // Do AQL query to get the uploaded files
        String aql = injectVariables(innerDir,
                "/specs/integrationTestSpecs/uploadTest-parenthesisAql.json", "", false);
        Set<DownloadableArtifact> foundArtifacts = performAql(aql);


        // Asserting returned results length
        assert foundArtifacts.size() == uploadedArtifacts.size() :
                String.format("Number of found in Artifactory and uploaded by the test artifacts should be equal, " +
                                "but uploaded - %d found in Artifactory - %d",
                        uploadedArtifacts.size(), foundArtifacts.size());

        // Assert all expected files are exists
        List<String> expectedResults = getExpectedResults(ConstData.UPLOAD_PARENTHESIS_RESULTS, innerDir);
        List<String> foundPaths = getFoundPaths(foundArtifacts);
        for (String expectedResult : expectedResults) {
            assert foundPaths.contains(expectedResult) :
                    String.format("Missing file! \nFile %s expected to be downloaded but it is not!",
                            expectedResult);
        }
    }

    /**
     * Performing a massive download with multiple flags combinations.
     * Every spec in "downloadTest-downloadSpec.json" represents a test case and should have a running serial number.
     * The test numeration is done to make investigation simpler.
     * As preparation the test uploads files by "downloadTest-uploadSpec.json" and then performs the download tests using
     * "downloadTest-downloadSpec.json".
     * In the end it compares the expected paths with the created in the temp directory paths.
     */
    public void testDownloadSpec() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        String innerDir = "download-test";
        // Class.getResource() returns windows path with slash prefix which brakes the upload. therefor the file creation and then the get path
        String workspace = new File(this.getClass().getResource("/workspace").getPath()).getCanonicalFile().getPath();

        // Prepare Artifactory for the download test
        File artifactsDir = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();
        String uploadSpec = injectVariables(innerDir, "/specs/integrationTestSpecs/downloadTest-uploadSpec.json", workspace, false);
        specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsDir, null, getBuildInfoClient());

        // Execute the download
        String downloadDestinationDir = System.getProperty("java.io.tmpdir") + SEPARATOR + TEST_SPACE + SEPARATOR + innerDir;
        String downloadSpec = injectVariables(innerDir, "/specs/integrationTestSpecs/downloadTest-downloadSpec.json", "", false);
        List<Dependency> downloadArtifactsBySpec =
                specsHelper.downloadArtifactsBySpec(downloadSpec, getDependenciesClient(), downloadDestinationDir);

        // Asserting returned results length
        assert downloadArtifactsBySpec.size() == ConstData.DOWNLOAD_RESULTS.length :
                String.format("Number of downloaded and expected by the test artifacts should be equal, " +
                                "but downloaded - %d artifacts and expected - %d",
                        downloadArtifactsBySpec.size(), ConstData.DOWNLOAD_RESULTS.length);

        // Assert all expected files are exists
        File workingDir = new File(TEST_WORKSPACE + SEPARATOR + innerDir).getCanonicalFile();
        Collection<File> downloadedFiles = FileUtils.listFiles(workingDir, null, true);
        for (String path : ConstData.DOWNLOAD_RESULTS) {
            assert downloadedFiles.contains(new File(TEST_WORKSPACE + SEPARATOR + innerDir + SEPARATOR + path).getCanonicalFile()) :
                    String.format("Missing file! \nFile %s expected to be downloaded but it is not!",
                            TEST_WORKSPACE + SEPARATOR + innerDir + SEPARATOR + path);
        }

        // Assert no other files where downloaded to the filesystem
        assert ConstData.DOWNLOAD_RESULTS.length == downloadedFiles.size() :
                String.format("Number of downloaded files from the upload test and the expected results should be equal, " +
                                "but expected - %d artifacts and downloaded - %d",
                        ConstData.DOWNLOAD_RESULTS.length, downloadedFiles.size());
    }

    /**
     * Tests upload specs that has no inner path in the repo. (uploads to the root of the repo)
     */
    public void testUploadSpecToRepoRoot() throws URISyntaxException, IOException, NoSuchAlgorithmException {
        File artifactsDir = new File(this.getClass().getResource("/workspace").toURI()).getCanonicalFile();

        // Class.getResource() returns windows path with slash prefix which brakes the upload. therefor the file creation and then the get path
        String workspace = new File(this.getClass().getResource("/workspace").getPath()).getCanonicalFile().getPath();

        // Execute upload to repos root
        String uploadSpec = injectVariables("", "/specs/integrationTestSpecs/uploadTest-uploadSpecToRepoRoot.json", workspace, false);
        List<Artifact> uploadedArtifacts = specsHelper.uploadArtifactsBySpec(uploadSpec, artifactsDir, null, getBuildInfoClient());

        // Do AQL query to get the uploaded files
        String aql = injectVariables("", "/specs/integrationTestSpecs/uploadTest-repoRootAql.json", "", false);
        Set<DownloadableArtifact> foundArtifacts = performAql(aql);

        // Asserting returned results length
        assert foundArtifacts.size() == uploadedArtifacts.size() :
                String.format("Number of found in Artifactory and uploaded by the test artifacts should be equal, " +
                                "but uploaded - %d found in Artifactory - %d",
                        uploadedArtifacts.size(), foundArtifacts.size());

        // Assert all expected files are exists
        List<String> expectedResults = Lists.newArrayList(ConstData.UPLOAD_REPO_ROOT_RESULTS);
        List<String> foundPaths = getFoundPaths(foundArtifacts);
        for (String expectedResult : expectedResults) {
            assert foundPaths.contains(expectedResult) :
                    String.format("Missing file! \nFile %s expected to be downloaded but it is not!", expectedResult);
        }

        // Cleanup
        deletePathsFromArtifactory(getRootObjects(foundPaths));
    }

    private List<String> getRootObjects(List<String> foundPaths) {
        List<String> result = new ArrayList<String>();
        for (String path : foundPaths) {
            result.add(StringUtils.substringBefore(path, "/"));
        }
        return result;
    }

    private Set<DownloadableArtifact> performAql(String aql) throws IOException {
        DependenciesDownloaderImpl dependenciesDownloader =
                new DependenciesDownloaderImpl(getDependenciesClient(), "", log);
        AqlDependenciesHelper aqlDependenciesHelper =
                new AqlDependenciesHelper(dependenciesDownloader, "", log);
        return aqlDependenciesHelper.collectArtifactsToDownload(aql);
    }

    private List<String> getExpectedResults(String[] uploadParenthesisResults, String innerDir) {
        List<String> result = new ArrayList<String>();
        for (String path : uploadParenthesisResults) {
            result.add(TEST_SPACE + "/" + innerDir + "/" + path);
        }
        return result;
    }

    private List<String> getFoundPaths(Set<DownloadableArtifact> foundArtifacts) {
        List<String> result = new ArrayList<String>();
        for (DownloadableArtifact foundArtifact : foundArtifacts) {
            result.add(foundArtifact.getFilePath());
        }
        return result;
    }

    private String injectVariables(String innerFolder, String pathToBaseSpecFile, String workspace, boolean isRegexp)
            throws URISyntaxException, IOException {
        File specFile = new File(this.getClass().getResource(pathToBaseSpecFile).toURI()).getCanonicalFile();

        String spec = FileUtils.readFileToString(specFile);
        spec = StringUtils.replace(spec, BITESTS_ARTIFACTORY_REPOSITORY_PLACEHOLDER, repo);
        spec = StringUtils.replace(spec, BITESTS_ARTIFACTORY_TEMP_FOLDER_PLACEHOLDER, System.getProperty("java.io.tmpdir"));
        // In case of windows regexp upload paths backslashes should be escaped
        if (isRegexp) {
            spec = StringUtils.replace(spec, "${WORKSPACE}", escapeSpecialChars(workspace));
        } else {
            spec = StringUtils.replace(spec, "${WORKSPACE}", workspace);
        }
        return StringUtils.replace(spec, "${TEST_SPACE}", TEST_SPACE + "/" + innerFolder);
    }


    @BeforeTest
    @AfterTest
    private void cleanup(ITestContext context) {
        boolean testFailed = false;
        for (Object failedTest : context.getFailedTests().getAllResults().toArray()){
            if (failedTest.toString().contains(this.getClass().getSimpleName())) {
                testFailed = true;
                break;
            }
        }
        // Cleanup will be run only if all tests passes
        if (!testFailed) {
            try {
                // Clean working directory
                FileUtils.deleteDirectory(new File(TEST_WORKSPACE));
                // Clean working Artifactory repository
                deleteItemFromArtifactory(TEST_SPACE);
            } catch (IOException ignored) {
            }
        }
    }

    private void deletePathsFromArtifactory(List<String> paths) {
        for (String path : paths) {
            try {
                deleteItemFromArtifactory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}