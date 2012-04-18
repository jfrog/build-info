package org.jfrog.build.api.dependency;

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

    public DownloadableArtifact() {
    }

    public DownloadableArtifact(String repoUrl, String relativeDirPath, String filePath, String matrixParameters) {
        this.repoUrl = repoUrl;
        this.relativeDirPath = relativeDirPath;
        this.filePath = filePath;
        this.matrixParameters = matrixParameters;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getRelativeDirPath() {
        return relativeDirPath;
    }

    public void setRelativeDirPath(String relativeDirPath) {
        this.relativeDirPath = relativeDirPath;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMatrixParameters() {
        return matrixParameters;
    }

    public void setMatrixParameters(String matrixParameters) {
        this.matrixParameters = matrixParameters;
    }
}
