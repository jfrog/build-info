package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.InputMismatchException;
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

        for (FileSpec file : downloadSpec.getFiles()) {
            log.debug("Downloading dependencies using spec: \n" + file.toString());
            validateFileSpec(file);
            String buildName = getBuildName(file.getBuild());
            String buildNumber = getBuildNumber(buildName, file.getBuild());
            if (StringUtils.isNotBlank(buildName) && StringUtils.isBlank(buildNumber)) {
                return resolvedDependencies;
            }
            if (file.getPattern() != null) {
                wildcardHelper.setTarget(file.getTarget());
                wildcardHelper.setFlatDownload(BooleanUtils.toBoolean(file.getFlat()));
                wildcardHelper.setRecursive(!"false".equalsIgnoreCase(file.getRecursive()));
                wildcardHelper.setProps(file.getProps());
                wildcardHelper.setBuildName(buildName);
                wildcardHelper.setBuildNumber(buildNumber);
                resolvedDependencies.addAll(wildcardHelper.retrievePublishedDependencies(file.getPattern(), Boolean.valueOf(file.getExplode())));
            } else if (file.getAql() != null) {
                aqlHelper.setTarget(file.getTarget());
                aqlHelper.setFlatDownload(BooleanUtils.toBoolean(file.getFlat()));
                aqlHelper.setBuildName(buildName);
                aqlHelper.setBuildNumber(buildNumber);
                resolvedDependencies.addAll(aqlHelper.retrievePublishedDependencies(file.getAql(), Boolean.valueOf(file.getExplode())));
            }
        }
        return resolvedDependencies;
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
                throw new IllegalStateException("build '" + build + "' does not start with build name '" + buildName + "'.");
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
                    throw new IllegalArgumentException(buildNumber + " is not supported in Artifactory OSS.");
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

    private void validateFileSpec(FileSpec file) throws IOException {
        if (file.getPattern() != null && file.getAql() != null ) {
            throw new InputMismatchException("Spec can include either the 'aql' or 'pattern' properties, but not both.");
        }
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


    private Dependency downloadArtifact(DownloadableArtifact downloadableArtifact) throws IOException {
        Dependency dependencyResult = null;
        String filePath = downloadableArtifact.getFilePath();
        String matrixParams = downloadableArtifact.getMatrixParameters();
        final String uri = downloadableArtifact.getRepoUrl() + '/' + filePath;
        final String uriWithParams = (StringUtils.isBlank(matrixParams) ? uri : uri + ';' + matrixParams);

        Checksums checksums = downloadArtifactCheckSums(uriWithParams);
        // If Artifactory returned no checksums, this is probably because the URL points to a folder,
        // so there's no need to download it.
        if (StringUtils.isBlank(checksums.getMd5()) && StringUtils.isBlank(checksums.getSha1())) {
            return null;
        }

        String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
            downloadableArtifact.getRelativeDirPath());
        dependencyResult = getDependencyLocally(checksums, fileDestination);
        if (dependencyResult == null) {
            log.info("Downloading '" + uriWithParams + "' ...");
            HttpResponse httpResponse = downloader.getClient().downloadArtifact(uriWithParams);
            InputStream inputStream = httpResponse.getEntity().getContent();
            Map<String, String> checksumsMap = downloader.saveDownloadedFile(inputStream, fileDestination);

            // If the checksums map is null then something went wrong and we should fail the build
            if (checksumsMap == null) {
                throw new IOException("Received null checksums map for downloaded file.");
            }

            String md5 = validateMd5Checksum(httpResponse, checksumsMap.get("md5"));
            String sha1 = validateSha1Checksum(httpResponse, checksumsMap.get("sha1"));

            log.info("Successfully downloaded '" + uriWithParams + "' to '" + fileDestination + "'");
            dependencyResult = new DependencyBuilder().md5(md5).sha1(sha1)
                    .id(filePath.substring(filePath.lastIndexOf("/")+1)).build();
        }

        return dependencyResult;
    }

    /**
     * Returns the dependency if it exists locally and has the sent checksums.
     * Otherwise return null.
     *
     * @param checksums     The artifact checksums returned from Artifactory.
     * @param filePath      The locally file path
     */
    private Dependency getDependencyLocally(Checksums checksums, String filePath) throws IOException {
        if (downloader.isFileExistsLocally(filePath, checksums.getMd5(), checksums.getSha1())) {
            log.info("The file '" + filePath + "' exists locally.");
            return new DependencyBuilder().md5(checksums.getMd5()).sha1(checksums.getSha1())
                    .id(filePath.substring(filePath.lastIndexOf(String.valueOf(IOUtils.DIR_SEPARATOR))+1)).build();
        }
        return null;
    }

    private Checksums downloadArtifactCheckSums(String url) throws IOException {
        HttpResponse response = downloader.getClient().getArtifactChecksums(url);

        Checksums checksums = new Checksums();
        checksums.setMd5(getMD5ChecksumFromResponse(response));
        checksums.setSha1(getSHA1ChecksumFromResponse(response));

        return checksums;
    }

    private String validateMd5Checksum(HttpResponse httpResponse, String calculatedMd5) throws IOException {
        String md5ChecksumFromResponse = getMD5ChecksumFromResponse(httpResponse);
        if (!StringUtils.equals(getMD5ChecksumFromResponse(httpResponse), calculatedMd5)) {
            String errorMessage = "Calculated MD5 checksum is different from original, "
                    + "Original: '" + md5ChecksumFromResponse + "' Calculated: '" + calculatedMd5 + "'";
            throw new IOException(errorMessage);
        }
        return md5ChecksumFromResponse == null ? "" : md5ChecksumFromResponse;
    }

    private String validateSha1Checksum(HttpResponse httpResponse, String calculatedSha1) throws IOException {
        String sha1ChecksumFromResponse = getSHA1ChecksumFromResponse(httpResponse);
        if (!StringUtils.equals(sha1ChecksumFromResponse, calculatedSha1)) {
            String errorMessage = "Calculated SHA-1 checksum is different from original, "
                    + "Original: '" + sha1ChecksumFromResponse + "' Calculated: '" + calculatedSha1 + "'";
            throw new IOException(errorMessage);
        }
        return sha1ChecksumFromResponse == null ? "" : sha1ChecksumFromResponse;
    }

    private String getSHA1ChecksumFromResponse(HttpResponse artifactChecksums) {
        String sha1 = null;
        Header sha1Header = artifactChecksums.getFirstHeader("X-Checksum-Sha1");
        if (sha1Header != null) {
            sha1 = sha1Header.getValue();
        }
        return sha1;
    }

    private String getMD5ChecksumFromResponse(HttpResponse artifactChecksums) {
        String md5 = null;
        Header md5Header = artifactChecksums.getFirstHeader("X-Checksum-Md5");
        if (md5Header != null) {
            md5 = md5Header.getValue();
        }
        return md5;
    }

    private static class Checksums {
        private String sha1;
        private String md5;

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
    }
}
