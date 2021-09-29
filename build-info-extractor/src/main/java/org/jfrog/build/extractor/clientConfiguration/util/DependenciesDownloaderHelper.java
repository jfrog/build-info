package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.ZipUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.filespecs.FileSpec;
import org.jfrog.filespecs.entities.FilesGroup;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.Upload.MD5_HEADER_NAME;
import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.Upload.SHA1_HEADER_NAME;

/**
 * Helper class for downloading dependencies
 *
 * @author Shay Yaakov
 */
public class DependenciesDownloaderHelper {

    public static final String SHA1_ALGORITHM_NAME = "sha1";
    public static final String MD5_ALGORITHM_NAME = "md5";

    private final DependenciesDownloader downloader;
    private final Log log;
    /**
     * Number of threads to use when downloading an artifact concurrently
     */
    private static final int CONCURRENT_DOWNLOAD_THREADS = 3;
    /**
     * Minimum file size for concurrent download
     */
    public static final int MIN_SIZE_FOR_CONCURRENT_DOWNLOAD = 5120000;

    public DependenciesDownloaderHelper(DependenciesDownloader downloader, Log log) {
        this.downloader = downloader;
        this.log = log;
    }

    public DependenciesDownloaderHelper(ArtifactoryManager artifactoryManager, String workingDirectory, Log log) {
        this.downloader = new DependenciesDownloaderImpl(artifactoryManager, workingDirectory, log);
        this.log = log;
    }

    /**
     * Download dependencies by the provided spec using the provided in the constructor client.
     * returns list of downloaded artifacts
     *
     * @param downloadSpec the download spec
     * @return list of downloaded artifacts
     * @throws IOException in case of IO error
     */
    public List<Dependency> downloadDependencies(FileSpec downloadSpec) throws IOException {
        ArtifactorySearcher searcher = new ArtifactorySearcher(downloader.getArtifactoryManager(), log);
        Set<DownloadableArtifact> downloadableArtifacts;
        List<AqlSearchResult.SearchEntry> searchResults;
        List<Dependency> resolvedDependencies = new ArrayList<>();

        for (FilesGroup file : downloadSpec.getFiles()) {
            log.debug("Downloading dependencies using spec: \n" + file.toString());
            this.downloader.setFlatDownload(BooleanUtils.toBoolean(file.getFlat()));
            searchResults = searcher.SearchByFileSpec(file);
            downloadableArtifacts = fetchDownloadableArtifactsFromResult(searchResults, Boolean.valueOf(file.getExplode()), file.getTarget());
            if (file.getSpecType() == FilesGroup.SpecType.PATTERN) {
                replaceTargetPlaceholders(file.getPattern(), downloadableArtifacts, file.getTarget());
            }
            resolvedDependencies.addAll(downloadDependencies(downloadableArtifacts));
        }
        return resolvedDependencies;
    }

    private void replaceTargetPlaceholders(String searchPattern, Set<DownloadableArtifact> downloadableArtifacts, String target) {
        searchPattern = StringUtils.substringAfter(searchPattern, "/");
        Pattern pattern = Pattern.compile(PathsUtils.pathToRegExp(searchPattern));
        target = StringUtils.defaultIfEmpty(target, "");
        for (DownloadableArtifact artifact : downloadableArtifacts) {
            if (StringUtils.isEmpty(target) || target.endsWith("/")) {
                artifact.setTargetDirPath(PathsUtils.reformatRegexp(artifact.getFilePath(), target, pattern));
            } else {
                String targetAfterReplacement = PathsUtils.reformatRegexp(artifact.getFilePath(), target, pattern);
                Map<String, String> targetFileName = PathsUtils.replaceFilesName(targetAfterReplacement, artifact.getRelativeDirPath());
                artifact.setRelativeDirPath(targetFileName.get("srcPath"));
                artifact.setTargetDirPath(targetFileName.get("targetPath"));
            }
        }
    }

    /**
     * Converts the found results to DownloadableArtifact types before downloading.
     */
    private Set<DownloadableArtifact> fetchDownloadableArtifactsFromResult(List<AqlSearchResult.SearchEntry> searchResults, boolean explode, String target) {
        Set<DownloadableArtifact> downloadableArtifacts = new HashSet<>();
        for (AqlSearchResult.SearchEntry searchEntry : searchResults) {
            String path = searchEntry.getPath().equals(".") ? "" : searchEntry.getPath() + "/";
            DownloadableArtifact downloadableArtifact = new DownloadableArtifact(searchEntry.getRepo(), target, path + searchEntry.getName(), "", "", PatternType.NORMAL);
            downloadableArtifact.setExplode(explode);
            downloadableArtifacts.add(downloadableArtifact);
        }
        return downloadableArtifacts;
    }

    public List<Dependency> downloadDependencies(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        log.info("Beginning to resolve Build Info published dependencies.");
        List<Dependency> dependencies = new ArrayList<>();
        Set<DownloadableArtifact> downloadedArtifacts = new HashSet<>();
        for (DownloadableArtifact downloadableArtifact : downloadableArtifacts) {
            Dependency dependency = downloadArtifact(downloadableArtifact);
            if (dependency != null) {
                dependencies.add(dependency);
                downloadedArtifacts.add(downloadableArtifact);
                explodeDependenciesIfNeeded(downloadableArtifact);
            }
        }

        removeUnusedArtifactsFromLocal(downloadedArtifacts);
        log.info("Finished resolving Build Info published dependencies.");
        return dependencies;
    }

    private void explodeDependenciesIfNeeded(DownloadableArtifact downloadableArtifact) throws IOException {
        if (!downloadableArtifact.isExplode()) {
            return;
        }
        String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
                downloadableArtifact.getRelativeDirPath());
        log.info("Extracting Archive: " + fileDestination);
        File sourceArchive = new File(fileDestination);
        File parentFile = FileUtils.getFile(fileDestination).getParentFile();
        ZipUtils.extract(sourceArchive, parentFile);
        log.info("Finished extracting archive to " + parentFile);
        log.debug("Deleting archive...");
        org.apache.commons.io.FileUtils.deleteQuietly(sourceArchive);
    }

    private void removeUnusedArtifactsFromLocal(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        Set<String> forDeletionFiles = new HashSet<>();
        Set<String> allResolvesFiles = new HashSet<>();
        for (DownloadableArtifact downloadableArtifact : downloadableArtifacts) {
            String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
                    downloadableArtifact.getRelativeDirPath());
            allResolvesFiles.add(fileDestination);
            if (PatternType.DELETE.equals(downloadableArtifact.getPatternType())) {
                forDeletionFiles.add(fileDestination);
            }
        }

        downloader.removeUnusedArtifactsFromLocal(allResolvesFiles, forDeletionFiles);
    }

    /**
     * Get artifact metadata and download the artifact if it's not a directory.
     *
     * @param downloadableArtifact download recipe
     * @return artifact dependency
     */
    private Dependency downloadArtifact(DownloadableArtifact downloadableArtifact) throws IOException {
        String filePath = downloadableArtifact.getFilePath();
        String matrixParams = downloadableArtifact.getMatrixParameters();
        String uri = downloadableArtifact.getRepoUrl() + '/' + filePath;
        final String uriWithParams = (StringUtils.isBlank(matrixParams) ? uri : uri + ';' + matrixParams);

        ArtifactMetaData artifactMetaData = downloadArtifactMetaData(uriWithParams);
        // If Artifactory returned no fileMetaData, this is probably because the URL points to a folder,
        // so there's no need to download it.
        if (StringUtils.isBlank(artifactMetaData.getMd5()) && StringUtils.isBlank(artifactMetaData.getSha1())) {
            return null;
        }
        return downloadArtifact(downloadableArtifact, artifactMetaData, uriWithParams, filePath);
    }

    /**
     * Download artifact.
     *
     * @param downloadableArtifact download recipe
     * @param artifactMetaData     the artifact metadata
     * @param uriWithParams        full artifact uri with matrix params
     * @param filePath             the path to file in file system
     * @return artifact dependency
     */
    Dependency downloadArtifact(DownloadableArtifact downloadableArtifact, ArtifactMetaData artifactMetaData, String uriWithParams, String filePath) throws IOException {
        String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
                downloadableArtifact.getRelativeDirPath());
        String remotePath = downloadableArtifact.getRepoUrl() + "/" + filePath;
        Dependency dependencyResult = getDependencyLocally(artifactMetaData, fileDestination, remotePath);

        if (dependencyResult != null) {
            return dependencyResult;
        }

        try {
            log.info(String.format("Downloading '%s'...", uriWithParams));
            Map<String, String> checksumsMap = artifactMetaData.getSize() >= MIN_SIZE_FOR_CONCURRENT_DOWNLOAD && artifactMetaData.isAcceptRange()
                    ? downloadFileConcurrently(uriWithParams, artifactMetaData.getSize(), fileDestination, filePath)
                    : downloadFile(uriWithParams, fileDestination);

            // If the checksums map is null then something went wrong and we should fail the build
            if (checksumsMap == null) {
                throw new IOException("Received null checksums map for downloaded file.");
            }

            dependencyResult = validateChecksumsAndBuildDependency(checksumsMap, artifactMetaData, filePath, fileDestination, remotePath);
            log.info(String.format("Successfully downloaded '%s' to '%s'", uriWithParams, fileDestination));

            return dependencyResult;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    protected Map<String, String> downloadFile(String downloadPath, String fileDestination) throws IOException {
        File downloadedFile = downloader.getArtifactoryManager().downloadToFile(downloadPath, fileDestination);
        try {
            return FileChecksumCalculator.calculateChecksums(downloadedFile, MD5_ALGORITHM_NAME, SHA1_ALGORITHM_NAME);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("Could not find checksum algorithm: %s", e.getLocalizedMessage()), e);
        }
    }

    /**
     * Download an artifact using {@link #CONCURRENT_DOWNLOAD_THREADS} multiple threads.
     * This method will be used for artifacts of size larger than {@link #MIN_SIZE_FOR_CONCURRENT_DOWNLOAD}.
     *
     * @param uriWithParams   the request uri
     * @param fileSize        in bytes, used for setting the download ranges
     * @param fileDestination location of saving the downloaded file in the file system
     * @param filePath        path of the downloaded file
     * @return checksums map of the downloaded artifact
     */
    protected Map<String, String> downloadFileConcurrently(final String uriWithParams, long fileSize, final String fileDestination, String filePath)
            throws Exception {
        String[] downloadedFilesPaths;
        File tempDir = Files.createTempDir();
        String tempPath = tempDir.getPath() + File.separatorChar + filePath;
        try {
            downloadedFilesPaths = doConcurrentDownload(fileSize, uriWithParams, tempPath);
            try (InputStream inputStream = concatenateFilesToSingleStream(downloadedFilesPaths)) {
                return downloader.saveDownloadedFile(inputStream, fileDestination);
            }
        } finally {
            FileUtils.deleteDirectory(tempDir);
        }
    }

    private String[] doConcurrentDownload(long fileSize, final String downloadPath, String tempPath)
            throws Exception {
        final MutableBoolean errorOccurred = new MutableBoolean(false);
        String[] downloadedFilesPaths = new String[CONCURRENT_DOWNLOAD_THREADS];
        long chunkSize = fileSize / CONCURRENT_DOWNLOAD_THREADS;
        Thread[] workers = new Thread[CONCURRENT_DOWNLOAD_THREADS];

        long start = 0;
        long end = chunkSize + fileSize % CONCURRENT_DOWNLOAD_THREADS - 1;
        for (int i = 0; i < CONCURRENT_DOWNLOAD_THREADS; i++) {
            final Map<String, String> headers = new HashMap<>();
            headers.put(HttpHeaders.RANGE, "bytes=" + start + "-" + end);

            final String fileDestination = tempPath + i;
            downloadedFilesPaths[i] = fileDestination;
            workers[i] = new Thread(() -> {
                try {
                    downloader.getArtifactoryManager().downloadToFile(downloadPath, fileDestination, headers);
                } catch (Exception e) {
                    errorOccurred.setValue(true);
                    printErrorToLog(e, fileDestination, downloadPath);
                }
            });
            workers[i].setName("downloader_" + i);
            workers[i].start();

            start = end + 1;
            end += chunkSize;
        }

        for (Thread worker : workers) {
            worker.join();
        }

        // Check if error occurred while downloading
        if (errorOccurred.booleanValue()) {
            throw new Exception(String.format("Error occurred while downloading %s, please refer to logs for more information", downloadPath));
        }

        return downloadedFilesPaths;
    }

    /**
     * Create a single InputStream.
     * The stream is constructed from the multiple provided file paths.
     *
     * @param downloadedFilesPaths String array containing paths to the downloaded files
     * @return single InputStream of the downloaded file
     */
    private InputStream concatenateFilesToSingleStream(String[] downloadedFilesPaths) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(downloadedFilesPaths[0]);
        if (downloadedFilesPaths.length < 2) {
            return inputStream;
        }

        for (int i = 1; i < downloadedFilesPaths.length; i++) {
            inputStream = new SequenceInputStream(
                    inputStream,
                    new FileInputStream(downloadedFilesPaths[i])
            );
        }
        return inputStream;
    }

    /**
     * Returns the dependency if it exists locally and has the sent fileMetaData.
     * Otherwise return null.
     *
     * @param fileMetaData The artifact fileMetaData returned from Artifactory.
     * @param localPath    The local file path
     * @param remotePath   The remote file path
     */
    private Dependency getDependencyLocally(ArtifactMetaData fileMetaData, String localPath, String remotePath) throws IOException {
        if (downloader.isFileExistsLocally(localPath, fileMetaData.getMd5(), fileMetaData.getSha1())) {
            log.info(String.format("The file '%s' exists locally.", localPath));
            return new DependencyBuilder()
                    .md5(fileMetaData.getMd5())
                    .sha1(fileMetaData.getSha1())
                    .id(localPath.substring(localPath.lastIndexOf(String.valueOf(IOUtils.DIR_SEPARATOR)) + 1))
                    .localPath(localPath)
                    .remotePath(remotePath)
                    .build();
        }
        return null;
    }

    protected ArtifactMetaData downloadArtifactMetaData(String url) throws IOException {
        try {
            ArtifactMetaData artifactMetaData = new ArtifactMetaData();
            for (Header header : downloader.getArtifactoryManager().downloadHeaders(url)) {
                String upperCaseHeader = StringUtils.upperCase(header.getName());
                if (MD5_HEADER_NAME.toUpperCase().equals(upperCaseHeader)) {
                    artifactMetaData.setMd5(header.getValue());
                } else if (SHA1_HEADER_NAME.toUpperCase().equals(upperCaseHeader)) {
                    artifactMetaData.setSha1(header.getValue());
                } else if (HttpHeaders.CONTENT_LENGTH.toUpperCase().equals(upperCaseHeader)) {
                    artifactMetaData.setSize(NumberUtils.toLong(header.getValue()));
                } else if (HttpHeaders.ACCEPT_RANGES.toUpperCase().equals(upperCaseHeader)) {
                    artifactMetaData.setAcceptRange("bytes".equalsIgnoreCase(header.getValue()));
                }
            }
            return artifactMetaData;
        } catch (NumberFormatException e) {
            throw new IOException(e);
        }
    }

    private String validateMd5Checksum(String metadataMd5, String calculatedMd5) throws IOException {
        if (!StringUtils.equals(metadataMd5, calculatedMd5)) {
            String errorMessage = String.format(
                    "Calculated MD5 checksum is different from original, Original: '%s' Calculated: '%s'",
                    metadataMd5, calculatedMd5);
            throw new IOException(errorMessage);
        }
        return metadataMd5 == null ? "" : metadataMd5;
    }

    private String validateSha1Checksum(String metadataSha1, String calculatedSha1) throws IOException {
        if (!StringUtils.equals(metadataSha1, calculatedSha1)) {
            String errorMessage = String.format(
                    "Calculated SHA-1 checksum is different from original, Original: '%s' Calculated: '%s'",
                    metadataSha1, calculatedSha1);
            throw new IOException(errorMessage);
        }
        return metadataSha1 == null ? "" : metadataSha1;
    }

    private Dependency validateChecksumsAndBuildDependency(Map<String, String> checksumsMap, ArtifactMetaData artifactMetaData, String filePath, String fileDestination, String remotePath)
            throws IOException {
        String md5 = validateMd5Checksum(artifactMetaData.getMd5(), checksumsMap.get(MD5_ALGORITHM_NAME));
        String sha1 = validateSha1Checksum(artifactMetaData.getSha1(), checksumsMap.get(SHA1_ALGORITHM_NAME));

        return new DependencyBuilder()
                .md5(md5)
                .sha1(sha1)
                .id(filePath.substring(filePath.lastIndexOf(File.separatorChar) + 1))
                .localPath(fileDestination)
                .remotePath(remotePath)
                .build();
    }

    public static File saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        // Create file
        File dest = new File(filePath);
        if (dest.exists()) {
            dest.delete();
        } else {
            dest.getParentFile().mkdirs();
        }

        // Save InputStream to file
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(dest);
            IOUtils.copyLarge(inputStream, fileOutputStream);

            return dest;
        } catch (IOException e) {
            throw new IOException(String.format("Could not create or write to file: %s", dest.getCanonicalPath()), e);
        } finally {
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
    }

    private void printErrorToLog(Exception e, String downloadPath, String uriWithParams) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        log.error(String.format("[Thread %s] downloading %s as part of file %s threw an exception: %s", Thread.currentThread().getName(), downloadPath, uriWithParams, sw.toString()));
    }

    protected static class ArtifactMetaData {
        private String sha1;
        private String md5;
        private long size;
        private boolean acceptRange;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getSha1() {
            return sha1;
        }

        public void setSha1(String sha1) {
            this.sha1 = sha1;
        }

        public String getMd5() {
            return md5;
        }

        public void setMd5(String md5) {
            this.md5 = md5;
        }

        public boolean isAcceptRange() {
            return acceptRange;
        }

        public void setAcceptRange(boolean acceptRange) {
            this.acceptRange = acceptRange;
        }
    }
}
