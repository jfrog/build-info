package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.FilesGroup;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper.ArtifactMetaData;
import static org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper.MD5_ALGORITHM_NAME;
import static org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper.MIN_SIZE_FOR_CONCURRENT_DOWNLOAD;
import static org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper.SHA1_ALGORITHM_NAME;

/**
 * Integration tests for the DependenciesDownloader classes.
 */
@Test
public class DownloadTest extends IntegrationTestsBase {
    private static final String TEST_REPO_PATH = "download_tests_folder";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), "download_tests_space");

    /**
     * Tests download of files - both bulk and concurrently.
     *
     * @param uploadedChecksum - checksums of the artifact uploaded to Artifactory, used to compare with the downloaded artifact.
     * @param fileName         - artifact name to download.
     * @param fileSize         - artifact size to download.
     */
    @Test(dataProvider = "testDownloadFilesProvider")
    public void testBulkAndConcurrentDownload(Map<String, String> uploadedChecksum, String fileName, long fileSize)
            throws Exception {
        String uriWithParams = localRepo1 + "/" + TEST_REPO_PATH + "/" + fileName;
        String fileDestination = tempWorkspace.getPath() + File.separatorChar + "download" + File.separatorChar + fileName;

        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(artifactoryManager, tempWorkspace.getPath(), log);
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

    @Test(dataProvider = "testDownloadFilesProvider")
    public void testDownloadArtifact(Map<String, String> uploadedChecksum, String fileName, long fileSize)
            throws Exception {
        DependenciesDownloaderHelper dependenciesDownloaderHelper = new DependenciesDownloaderHelper(artifactoryManager, ".", log);

        String repoUrl = localRepo1 + "/" + TEST_REPO_PATH;
        String targetDirPath = tempWorkspace.getPath() + File.separatorChar + "download" + File.separatorChar;
        String url = repoUrl + "/" + fileName;

        ArtifactMetaData artifactMetaData = dependenciesDownloaderHelper.downloadArtifactMetaData(url);
        Assert.assertEquals(artifactMetaData.getMd5(), uploadedChecksum.get(MD5_ALGORITHM_NAME));
        Assert.assertEquals(artifactMetaData.getSha1(), uploadedChecksum.get(SHA1_ALGORITHM_NAME));
        Assert.assertEquals(artifactMetaData.getSize(), fileSize);

        DownloadableArtifact downloadableArtifact = new DownloadableArtifact(repoUrl, targetDirPath, fileName, "", fileName, PatternType.NORMAL);
        Dependency dependency = dependenciesDownloaderHelper.downloadArtifact(downloadableArtifact, artifactMetaData, url, fileName);
        Assert.assertEquals(dependency.getId(), fileName);
        Assert.assertEquals(dependency.getMd5(), uploadedChecksum.get(MD5_ALGORITHM_NAME));
        Assert.assertEquals(dependency.getSha1(), uploadedChecksum.get(SHA1_ALGORITHM_NAME));
        Assert.assertEquals((new File(targetDirPath + fileName)).length(), fileSize);
    }

    @Test(dataProvider = "testDownloadFilesProvider")
    public void testDownloadArtifactWithoutContentLength(Map<String, String> uploadedChecksum, String fileName, long fileSize)
            throws Exception {
        DependenciesDownloaderHelper dependenciesDownloaderHelper = new DependenciesDownloaderHelper(artifactoryManager, ".", log);

        String repoUrl = localRepo1 + "/" + TEST_REPO_PATH;
        String targetDirPath = tempWorkspace.getPath() + File.separatorChar + "download" + File.separatorChar;
        String url = repoUrl + "/" + fileName;

        ArtifactMetaData artifactMetaData = dependenciesDownloaderHelper.downloadArtifactMetaData(url);
        Assert.assertEquals(artifactMetaData.getMd5(), uploadedChecksum.get(MD5_ALGORITHM_NAME));
        Assert.assertEquals(artifactMetaData.getSha1(), uploadedChecksum.get(SHA1_ALGORITHM_NAME));
        Assert.assertEquals(artifactMetaData.getSize(), fileSize);
        artifactMetaData.setSize(0); // When content-length is missing

        DownloadableArtifact downloadableArtifact = new DownloadableArtifact(repoUrl, targetDirPath, fileName, "", fileName, PatternType.NORMAL);
        Dependency dependency = dependenciesDownloaderHelper.downloadArtifact(downloadableArtifact, artifactMetaData, url, fileName);
        Assert.assertEquals(dependency.getId(), fileName);
        Assert.assertEquals(dependency.getMd5(), uploadedChecksum.get(MD5_ALGORITHM_NAME));
        Assert.assertEquals(dependency.getSha1(), uploadedChecksum.get(SHA1_ALGORITHM_NAME));
        Assert.assertEquals((new File(targetDirPath + fileName)).length(), fileSize);
    }

    public void testDownloadArtifactFromDifferentPath() throws IOException {
        String targetDirPath = tempWorkspace.getPath() + File.separatorChar + "testDownloaddupArtifactFromDifferentPath" + File.separatorChar;
        FileSpec fileSpec = new FileSpec();
        // Upload one file to different locations in Artifactory.
        try {
            File file = createRandomFile(tempWorkspace.getPath() + File.pathSeparatorChar + "file", 1);
            for (int i = 0; i < 3; i++) {
                String filePath = TEST_REPO_PATH + "/" + i + "/file";
                DeployDetails deployDetails = new DeployDetails.Builder()
                        .file(file)
                        .artifactPath(filePath)
                        .targetRepository(localRepo1)
                        .explode(false)
                        .packageType(DeployDetails.PackageType.GENERIC)
                        .build();
                FilesGroup fg = new FilesGroup();
                fg.setPattern(localRepo1 + "/" + filePath);
                fg.setTarget(targetDirPath);
                fileSpec.addFilesGroup(fg);
                // Upload artifact
                artifactoryManager.upload(deployDetails);
            }
            DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(artifactoryManager, tempWorkspace.getPath(), log);
            List<Dependency> dependencies = helper.downloadDependencies(fileSpec);
            Assert.assertEquals(dependencies.size(), 3);
        } finally {
            FileUtils.deleteDirectory(tempWorkspace);
        }
    }

    /**
     * Create and upload files to Artifactory.
     * The test files are created according to the data provided in testFilesMap
     */
    @DataProvider
    private Object[][] testDownloadFilesProvider() throws IOException, NoSuchAlgorithmException {
        if (artifactoryManager == null) {
            throw new IOException("tests were not initialized successfully. aborting");
        }
        Map<String, Integer> testFilesMap = new HashMap<String, Integer>() {{
            put("file1", MIN_SIZE_FOR_CONCURRENT_DOWNLOAD);
            put("file2", MIN_SIZE_FOR_CONCURRENT_DOWNLOAD - 1);
            put("zeroByte", 0);
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
                        .targetRepository(localRepo1)
                        .md5(checksum.get(MD5_ALGORITHM_NAME))
                        .sha1(checksum.get(SHA1_ALGORITHM_NAME))
                        .explode(false)
                        .packageType(DeployDetails.PackageType.GENERIC)
                        .build();

                // Upload artifact
                artifactoryManager.upload(deployDetails);
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
        deleteContentFromRepo(localRepo1);
    }
}
