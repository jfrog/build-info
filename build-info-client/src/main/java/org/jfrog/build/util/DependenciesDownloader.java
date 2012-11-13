package org.jfrog.build.util;

import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.client.ArtifactoryDependenciesClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Actual download performer, should hold an actual client and implement
 * it's own specific logic upon dependencies downloading
 *
 * @author Shay Yaakov
 */
public interface DependenciesDownloader {

    ArtifactoryDependenciesClient getClient();

    List<Dependency> download(Set<DownloadableArtifact> downloadableArtifacts) throws IOException;

    /**
     * Returns the full target dir of where the artifact is saved
     * Usually that can be a full relative path under the file system of the full relative
     * path to the user workspace.
     */
    String getTargetDir(String targetDir, String relativeDir);

    /**
     * Parse the given input stream, save it to an actual file to the given path and return
     * the calculated checksums for the saved file.
     * Implementers must close the given input stream when finished.
     */
    Map<String, String> saveDownloadedFile(InputStream is, String filePath) throws IOException;

    /**
     * Checks if the file path exists locally with the same MD5 and SHA-1 checksums.
     *
     * @param filePath The local file path to check
     * @param md5      The MD5 checksum to compare with
     * @param sha1     The SHA-1 checksum to compare with
     */
    boolean isFileExistsLocally(String filePath, String md5, String sha1) throws IOException;
}
