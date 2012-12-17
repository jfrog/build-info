package org.jfrog.build.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

    public DependenciesDownloaderHelper(DependenciesDownloader downloader, Log log) {
        this.downloader = downloader;
        this.log = log;
    }

    public List<Dependency> downloadDependencies(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        List<Dependency> dependencies = Lists.newArrayList();
        Set<DownloadableArtifact> downloadedArtifacts = Sets.newHashSet();
        for (DownloadableArtifact downloadableArtifact : downloadableArtifacts) {
            Dependency dependency = downloadArtifact(downloadableArtifact);
            if (dependency != null) {
                dependencies.add(dependency);
                downloadedArtifacts.add(downloadableArtifact);
            }
        }

        removeUnusedArtifactsFromLocal(downloadedArtifacts);
        return dependencies;
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
        String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
                downloadableArtifact.getRelativeDirPath());

        try {
            dependencyResult = getDependencyLocally(uriWithParams, fileDestination);
            if (dependencyResult == null) {
                log.info("Downloading '" + uriWithParams + "' ...");
                HttpResponse httpResponse = downloader.getClient().downloadArtifact(uriWithParams);
                InputStream inputStream = httpResponse.getEntity().getContent();
                Map<String, String> checksumsMap = downloader.saveDownloadedFile(inputStream, fileDestination);

                // If the checksums map is null then something went wrong and we should fail the build
                if (checksumsMap == null) {
                    throw new IOException("Received null checksums map");
                }

                String md5 = validateMd5Checksum(httpResponse, checksumsMap.get("md5"));
                String sha1 = validateSha1Checksum(httpResponse, checksumsMap.get("sha1"));

                log.info("Successfully downloaded '" + uriWithParams + "' to '" + fileDestination + "'");
                dependencyResult = new DependencyBuilder().id(filePath).md5(md5).sha1(sha1).build();
            }
        } catch (FileNotFoundException e) {
            if (StringUtils.isNotBlank(matrixParams)) {
                String skippedMessage = "Skipping download of '" + uriWithParams + "' due to matrix params mismatch.";
                log.debug(skippedMessage);
            } else {
                String warningMessage = "Error occurred while resolving published dependency: " + e.getMessage();
                log.warn(warningMessage);
            }
        }

        return dependencyResult;
    }

    /**
     * Perform HEAD request to get the artifact checksums and check if the local file is the same.
     *
     * @param uriWithParams The uri for the artifact to perform HEAD request to
     * @param filePath      The locally file path
     */
    private Dependency getDependencyLocally(String uriWithParams, String filePath) throws IOException {
        Dependency dependencyResult = null;
        HttpResponse artifactChecksums = downloader.getClient().getArtifactChecksums(uriWithParams);
        String md5 = getMD5ChecksumFromResponse(artifactChecksums);
        String sha1 = getSHA1ChecksumFromResponse(artifactChecksums);

        if (downloader.isFileExistsLocally(filePath, md5, sha1)) {
            log.debug("File '" + filePath + "' already exists locally, skipping remote download.");
            dependencyResult = new DependencyBuilder().id(filePath).md5(md5).sha1(sha1).build();
        }

        return dependencyResult;
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
}
