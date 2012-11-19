package org.jfrog.build.api.dependency;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * Represents an artifact to be downloaded, used by generic resolving.
 *
 * @author Shay Yaakov
 */
public class DownloadableArtifact implements Serializable {
    String repoUrl;
    String relativeDirPath;
    String filePath;
    String matrixParameters;
    private String sourcePattern;

    public DownloadableArtifact() {
    }

    public DownloadableArtifact(String repoUrl, String relativeDirPath, String filePath, String matrixParameters,
            String sourcePattern) {
        this.repoUrl = repoUrl;
        this.relativeDirPath = relativeDirPath;
        this.filePath = filePath;
        this.matrixParameters = matrixParameters;
        this.sourcePattern = extractRepoFromPattern(sourcePattern);
    }

    private String extractRepoFromPattern(String sourcePattern) {
        int indexOfColon = StringUtils.indexOf(sourcePattern, ":");
        if (indexOfColon == -1) {
            return sourcePattern;
        }

        return StringUtils.substring(sourcePattern, indexOfColon + 1, StringUtils.length(sourcePattern));
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getRelativeDirPath() {
        return relativeDirPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getMatrixParameters() {
        return matrixParameters;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }
}
