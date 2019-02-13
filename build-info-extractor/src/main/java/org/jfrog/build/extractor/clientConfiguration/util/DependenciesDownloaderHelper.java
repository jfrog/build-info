package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.ZipUtils;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.SpecsHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.DownloadSpecValidator;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class for downloading dependencies
 *
 * @author Shay Yaakov
 */
public class DependenciesDownloaderHelper {

    private DependenciesDownloader downloader;
    private Log log;
    private final String LATEST = "LATEST";
    private final String LAST_RELEASE = "LAST_RELEASE";
    private static final String DELIMITER = "/";
    private static final String ESCAPE_CHAR = "\\";
    private static final String MD5_HEADER_NAME = "X-Checksum-Md5";
    private static final String SHA1_HEADER_NAME = "X-Checksum-Sha1";
    public static final String MD5_ALGORITHM_NAME = "md5";
    public static final String SHA1_ALGORITHM_NAME = "sha1";
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

    public DependenciesDownloaderHelper(ArtifactoryDependenciesClient client, String workingDirectory, Log log) {
        this.downloader = new DependenciesDownloaderImpl(client, workingDirectory, log);
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
    public List<Dependency> downloadDependencies(Spec downloadSpec) throws IOException {
        AqlDependenciesHelper aqlHelper = new AqlDependenciesHelper(downloader, "", log);
        WildcardsDependenciesHelper wildcardHelper = new WildcardsDependenciesHelper(downloader, "", log);
        List<Dependency> resolvedDependencies = Lists.newArrayList();
        new DownloadSpecValidator().validate(downloadSpec);

        for (FileSpec file : downloadSpec.getFiles()) {
            log.debug("Downloading dependencies using spec: \n" + file.toString());
            switch(file.getSpecType()) {
                case PATTERN: {
                    setWildcardHelperProperties(wildcardHelper,file);
                    log.info(String.format("Downloading artifacts using pattern: %s%s", file.getPattern(), SpecsHelper.getExcludePatternsLogStr(file.getExcludePatterns())));
                    resolvedDependencies.addAll(wildcardHelper.retrievePublishedDependencies(file.getPattern(), file.getExcludePatterns(), Boolean.valueOf(file.getExplode())));
                    break;
                }
                case BUILD: {
                    setAqlHelperProperties(aqlHelper,file);
                    resolvedDependencies.addAll(aqlHelper.retrievePublishedDependenciesByBuildOnly(Boolean.valueOf(file.getExplode())));
                    break;
                }
                case AQL: {
                    setAqlHelperProperties(aqlHelper,file);
                    resolvedDependencies.addAll(aqlHelper.retrievePublishedDependencies(file.getAql(), null, Boolean.valueOf(file.getExplode())));
                    break;
                }
            }
        }
        return resolvedDependencies;
    }

    private void setWildcardHelperProperties(WildcardsDependenciesHelper wildcardHelper, FileSpec file) throws IOException{
        wildcardHelper.setTarget(file.getTarget());
        wildcardHelper.setFlatDownload(BooleanUtils.toBoolean(file.getFlat()));
        wildcardHelper.setRecursive(!"false".equalsIgnoreCase(file.getRecursive()));
        wildcardHelper.setProps(file.getProps());
        String buildName = getBuildName(file.getBuild());
        wildcardHelper.setBuildName(buildName);
        wildcardHelper.setBuildNumber(getBuildNumber(buildName, file.getBuild()));
    }

    private void setAqlHelperProperties(AqlDependenciesHelper aqlHelper, FileSpec file) throws IOException{
        aqlHelper.setTarget(file.getTarget());
        aqlHelper.setFlatDownload(BooleanUtils.toBoolean(file.getFlat()));
        String buildName = getBuildName(file.getBuild());
        aqlHelper.setBuildName(buildName);
        aqlHelper.setBuildNumber(getBuildNumber(buildName, file.getBuild()));
    }

    private String getBuildName(String build) {
        if (StringUtils.isBlank(build)) {
            return build;
        }
        // The delimiter must not be prefixed with escapeChar (if it is, it should be part of the build number)
        // the code below gets substring from before the last delimiter.
        // If the new string ends with escape char it means the last delimiter was part of the build number and we need
        // to go back to the previous delimiter.
        // If no proper delimiter was found the full string will be the build name.
        String buildName = StringUtils.substringBeforeLast(build, DELIMITER);
        while (StringUtils.isNotBlank(buildName) && buildName.contains(DELIMITER) && buildName.endsWith(ESCAPE_CHAR)) {
            buildName = StringUtils.substringBeforeLast(buildName, DELIMITER);
        }
        return buildName.endsWith(ESCAPE_CHAR) ? build : buildName;
    }

    public List<Dependency> downloadDependencies(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        List<Dependency> dependencies = Lists.newArrayList();
        Set<DownloadableArtifact> downloadedArtifacts = Sets.newHashSet();
        for (DownloadableArtifact downloadableArtifact : downloadableArtifacts) {
            Dependency dependency = downloadArtifact(downloadableArtifact);
            if (dependency != null) {
                dependencies.add(dependency);
                downloadedArtifacts.add(downloadableArtifact);
                explodeDependenciesIfNeeded(downloadableArtifact);
            }
        }

        removeUnusedArtifactsFromLocal(downloadedArtifacts);
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

    private String getBuildNumber(String buildName, String build) throws IOException {
        String buildNumber = "";
        if (StringUtils.isNotBlank(buildName)) {
            if (!build.startsWith(buildName)) {
                throw new IllegalStateException(String.format("build '%s' does not start with build name '%s'.", build, buildName));
            }
            // Case build number was not provided, the build name and the build are the same. build number will be latest
            if (build.equals(buildName)) {
                buildNumber = LATEST;
            } else {
                // Get build name by removing build name and the delimiter
                buildNumber = build.substring(buildName.length() + DELIMITER.length());
                // Remove the escape chars before the delimiters
                buildNumber = buildNumber.replace(ESCAPE_CHAR + DELIMITER, DELIMITER);
            }
            if (LATEST.equals(buildNumber.trim()) || LAST_RELEASE.equals(buildNumber.trim())) {
                if (downloader.getClient().isArtifactoryOSS()) {
                    throw new IllegalArgumentException(String.format("%s is not supported in Artifactory OSS.", buildNumber));
                }
                List<BuildPatternArtifactsRequest> artifactsRequest = Lists.newArrayList();
                artifactsRequest.add(new BuildPatternArtifactsRequest(buildName, buildNumber));
                List<BuildPatternArtifacts> artifactsResponses =
                        downloader.getClient().retrievePatternArtifacts(artifactsRequest);
                // Artifactory returns null if no build was found
                if (artifactsResponses.get(0) != null) {
                    buildNumber = artifactsResponses.get(0).getBuildNumber();
                } else {
                    logBuildNotFound(buildName, buildNumber);
                    return null;
                }
            }
        }
        return buildNumber;
    }

    private void logBuildNotFound(String buildName, String buildNumber) {
        StringBuilder sb = new StringBuilder("The build name ").append(buildName);
        if (LAST_RELEASE.equals(buildNumber.trim())) {
            sb.append(" with the status RELEASED");
        }
        sb.append(" could not be found.");
        log.warn(sb.toString());
    }

    private void removeUnusedArtifactsFromLocal(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        Set<String> forDeletionFiles = Sets.newHashSet();
        Set<String> allResolvesFiles = Sets.newHashSet();
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
     * @throws IOException
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
     * @throws IOException
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

    protected Map<String, String> downloadFile(String uriWithParams, String fileDestination)
            throws IOException {
        HttpResponse httpResponse = null;
        try {
            httpResponse = downloader.getClient().downloadArtifact(uriWithParams);
            InputStream inputStream = httpResponse.getEntity().getContent();
            return downloader.saveDownloadedFile(inputStream, fileDestination);
        } finally {
            consumeEntity(httpResponse);
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
        InputStream inputStream = null;
        try {
            String tempPath = tempDir.getPath() + File.separatorChar + filePath;
            downloadedFilesPaths = doConcurrentDownload(fileSize, uriWithParams, tempPath);
            inputStream = concatenateFilesToSingleStream(downloadedFilesPaths);

            return downloader.saveDownloadedFile(inputStream, fileDestination);
        } finally {
            FileUtils.deleteDirectory(tempDir);
            IOUtils.closeQuietly(inputStream);
        }
    }

    private String[] doConcurrentDownload(long fileSize, final String uriWithParams, String tempPath)
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

            final String downloadPath = tempPath + String.valueOf(i);
            downloadedFilesPaths[i] = downloadPath;
            workers[i] = new Thread(new Runnable() {
                public void run() {
                    try {
                        saveRequestToFile(uriWithParams, downloadPath, headers);
                    } catch (Exception e) {
                        errorOccurred.setValue(true);
                        printErrorToLog(e, downloadPath, uriWithParams);
                    }
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
            throw new Exception(String.format("Error occurred while downloading %s, please refer to logs for more information", uriWithParams));
        }

        return downloadedFilesPaths;
    }

    /**
     * Executes a GET request to download files and saves the result to the file system.
     * Used by the downloading threads of concurrentDownloadedArtifact.
     *
     * @param uriWithParams   the request uri
     * @param fileDestination location to save the downloaded file to
     * @param headers         additional headers for the request
     */
    private void saveRequestToFile(String uriWithParams, String fileDestination, Map<String, String> headers) throws IOException {
        HttpResponse httpResponse = null;
        try {
            httpResponse = downloader.getClient().downloadArtifact(uriWithParams, headers);
            InputStream inputStream = httpResponse.getEntity().getContent();
            saveInputStreamToFile(inputStream, fileDestination);
        } finally {
            consumeEntity(httpResponse);
        }
    }

    /**
     * Create a single InputStream.
     * The stream is constructed from the multiple provided file paths.
     *
     * @param downloadedFilesPaths String array containing paths to the downloaded files
     * @return single InputStream of the downloaded file
     */
    private InputStream concatenateFilesToSingleStream(String[] downloadedFilesPaths) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(new File(downloadedFilesPaths[0]));
        if (downloadedFilesPaths.length < 2) {
            return inputStream;
        }

        for (int i = 1; i < downloadedFilesPaths.length; i++) {
            inputStream = new SequenceInputStream(
                    inputStream,
                    new FileInputStream(new File(downloadedFilesPaths[i]))
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
        HttpResponse response = null;
        try {
            response = downloader.getClient().getArtifactMetadata(url);
            ArtifactMetaData artifactMetaData = new ArtifactMetaData();
            artifactMetaData.setMd5(getHeaderContentFromResponse(response, MD5_HEADER_NAME));
            artifactMetaData.setSha1(getHeaderContentFromResponse(response, SHA1_HEADER_NAME));
            artifactMetaData.setSize(NumberUtils.toLong(getHeaderContentFromResponse(response, HttpHeaders.CONTENT_LENGTH)));
            artifactMetaData.setAcceptRange("bytes".equals(getHeaderContentFromResponse(response, HttpHeaders.ACCEPT_RANGES)));
            return artifactMetaData;
        } catch (NumberFormatException e) {
            throw new IOException(e);
        } finally {
            consumeEntity(response);
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

    private String getHeaderContentFromResponse(HttpResponse response, String headerName) {
        String headerContent = null;
        Header header = response.getFirstHeader(headerName);
        if (header != null) {
            headerContent = header.getValue();
        }
        return headerContent;
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

    private static void consumeEntity(HttpResponse response) {
        if (response != null) {
            try {
                EntityUtils.consume(response.getEntity());
            } catch (IOException e) {
                // Ignore
            }
        }
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
