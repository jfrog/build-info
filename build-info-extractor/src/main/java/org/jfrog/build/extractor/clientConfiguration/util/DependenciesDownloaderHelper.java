package org.jfrog.build.extractor.clientConfiguration.util;

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

        Checksums checksums = downloadArtifactCheckSums(uriWithParams);
        // If Artifactory returned no checksums, this is probably because the URL points to a folder,
        // so there's no need to download it.
        if (StringUtils.isBlank(checksums.getMd5()) && StringUtils.isBlank(checksums.getSha1())) {
            return null;
        }

        String fileDestination = downloader.getTargetDir(downloadableArtifact.getTargetDirPath(),
            downloadableArtifact.getRelativeDirPath());

        try {
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
                dependencyResult = new DependencyBuilder().id(filePath).md5(md5).sha1(sha1).build();
            }
        } catch (IOException e) {
            log.warn("Error occurred while resolving published dependency: " + uriWithParams + " "  + e.getMessage());
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
            log.debug("File '" + filePath + "' already exists locally, skipping remote download.");
            return new DependencyBuilder().id(filePath).md5(checksums.getMd5()).sha1(checksums.getSha1()).build();
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
