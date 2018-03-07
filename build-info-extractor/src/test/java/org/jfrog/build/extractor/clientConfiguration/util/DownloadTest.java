package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper.*;

/**
 * Integration tests for the DependenciesDownloader classes.
 */
@Test
public class DownloadTest extends IntegrationTestsBase {
    private static final String TEST_REPO_PATH = "download_tests_folder";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), "download_tests_space");

    /**
     * Tests download of files - both bulk and concurrently.
     * @param uploadedChecksum checksums of the artifact uploaded to Artifactory, used to compare with the downloaded artifact.
     * @param fileName
     * @param fileSize
     */
    @Test(dataProvider = "testDownloadFilesProvider")
    public void testDownloadFiles(Map<String, String> uploadedChecksum, String fileName, long fileSize)
            throws IOException, InterruptedException {
        String uriWithParams = dependenciesClient.getArtifactoryUrl() + "/" + localRepo + "/" + TEST_REPO_PATH + "/" + fileName;
        String fileDestination =  tempWorkspace.getPath() + File.separatorChar + "download" + File.separatorChar + fileName;

        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(dependenciesClient, tempWorkspace.getPath(), log);
        DependenciesDownloaderHelper.ArtifactMetaData artifactMetaData = helper.downloadArtifactMetaData(uriWithParams);
        Assert.assertEquals(artifactMetaData.getSize(), fileSize);

        Map<String, String> downloadedChecksum;
        if (fileSize >= MIN_SIZE_FOR_CONCURRENT_DOWNLOAD && artifactMetaData.isAcceptRange()) {
            // Perform concurrent download.
            downloadedChecksum = helper.downloadFileConcurrently(
                    uriWithParams,
                    fileSize,
                    fileDestination,
                    fileName);
        } else {
            // Perform bulk download.
            downloadedChecksum = helper.downloadFile(
                    uriWithParams,
                    fileDestination);
        }

        // Verify the downloaded file.
        Assert.assertEquals(uploadedChecksum.get(MD5_ALGORITHM_NAME), downloadedChecksum.get(MD5_ALGORITHM_NAME));
        Assert.assertEquals(uploadedChecksum.get(SHA1_ALGORITHM_NAME), downloadedChecksum.get(SHA1_ALGORITHM_NAME));
    }

    /**
     * Create and upload files to Artifactory.
     * The test files are created according to the data provided in testFilesMap
     */
    @DataProvider
    private Object[][] testDownloadFilesProvider() throws IOException, NoSuchAlgorithmException {
        Map<String, Integer> testFilesMap = new HashMap<String, Integer>() {{
            put("file1", MIN_SIZE_FOR_CONCURRENT_DOWNLOAD);
            put("file2", MIN_SIZE_FOR_CONCURRENT_DOWNLOAD - 1);
        }};

        Object[][] tests = new Object[testFilesMap.size()][4];

        try {
            int i = 0;
            for (Map.Entry<String, Integer> entry : testFilesMap.entrySet()) {
                String fileName = entry.getKey();
                int fileSize = entry.getValue();

                // Create file and calculate checksum
                File file = createRandomFile(tempWorkspace.getPath() + File.pathSeparatorChar + fileName, fileSize);
                Map<String, String> checksum = FileChecksumCalculator.calculateChecksums(file, SHA1_ALGORITHM_NAME, MD5_ALGORITHM_NAME);

                DeployDetails deployDetails = new DeployDetails.Builder()
                        .file(file)
                        .artifactPath(TEST_REPO_PATH + "/" + fileName)
                        .targetRepository(localRepo)
                        .md5(checksum.get(MD5_ALGORITHM_NAME))
                        .sha1(checksum.get(SHA1_ALGORITHM_NAME))
                        .explode(false).build();

                // Upload artifact
                buildInfoClient.deployArtifact(deployDetails);
                long createdFileSize = deployDetails.getFile().length();

                tests[i] = new Object[]{checksum, fileName, createdFileSize};
                i++;
            }
        } finally {
            FileUtils.deleteDirectory(tempWorkspace);
        }

        return tests;
    }

    private File createRandomFile(String filePath, int fileSize) throws IOException {
        File file = new File(filePath);
        if (file.exists()) {
            file.delete();
        } else {
            file.getParentFile().mkdirs();
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            randomAccessFile.setLength(fileSize);
        }

        return file;
    }

    @BeforeClass
    @AfterClass
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo);
    }
}
