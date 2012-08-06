package org.jfrog.build.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.dependency.DownloadableArtifact;
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
        for (DownloadableArtifact downloadableArtifact : downloadableArtifacts) {
            Dependency dependency = downloadArtifact(downloadableArtifact.getRepoUrl(),
                    downloadableArtifact.getFilePath(),
                    downloadableArtifact.getMatrixParameters());
            if (dependency != null) {
                dependencies.add(dependency);
            }
        }

        return dependencies;
    }


    private Dependency downloadArtifact(String repoUri, String filePath, String matrixParams) throws IOException {
        Dependency dependencyResult = null;
        final String uri = repoUri + '/' + filePath;
        final String uriWithParams = (StringUtils.isBlank(matrixParams) ? uri : uri + ';' + matrixParams);

        log.info("Downloading '" + uriWithParams + "' ...");
        HttpResponse httpResponse = null;
        try {
            httpResponse = downloader.getClient().downloadArtifact(uriWithParams);
            InputStream inputStream = httpResponse.getEntity().getContent();
            Map<String, String> checksumsMap = downloader.saveDownloadedFile(inputStream, filePath);

            // If the checksums map is null then something went wrong and we should fail the build
            if (checksumsMap == null) {
                throw new IOException("Received null checksums map");
            }

            String md5 = validateMd5Checksum(httpResponse, checksumsMap.get("md5"));
            String sha1 = validateSha1Checksum(httpResponse, checksumsMap.get("sha1"));

            log.info("Successfully downloaded '" + uriWithParams + "' to '" + downloader.getTargetDir(filePath) + "'");
            dependencyResult = new DependencyBuilder().id(filePath).md5(md5).sha1(sha1).build();
        } catch (FileNotFoundException e) {
            String warningMessage = "Error occurred while resolving published dependency: " + e.getMessage();
            log.warn(warningMessage);
        }

        return dependencyResult;
    }

    private String validateMd5Checksum(HttpResponse httpResponse, String calculatedMd5) throws IOException {
        Header md5Header = httpResponse.getFirstHeader("X-Checksum-Md5");
        String md5 = "";
        if (md5Header != null) {
            md5 = md5Header.getValue();
            if (!StringUtils.equals(md5, calculatedMd5)) {
                String errorMessage = "Calculated MD5 checksum is different from original, "
                        + "Original: '" + md5 + "' Calculated: '" + calculatedMd5 + "'";
                throw new IOException(errorMessage);
            }
        }
        return md5;
    }

    private String validateSha1Checksum(HttpResponse httpResponse, String calculatedSha1) throws IOException {
        Header sha1Header = httpResponse.getFirstHeader("X-Checksum-Sha1");
        String sha1 = "";
        if (sha1Header != null) {
            sha1 = sha1Header.getValue();
            if (!StringUtils.equals(sha1, calculatedSha1)) {
                String errorMessage = "Calculated SHA-1 checksum is different from original, "
                        + "Original: '" + sha1 + "' Calculated: '" + calculatedSha1 + "'";
                throw new IOException(errorMessage);
            }
        }
        return sha1;
    }
}
