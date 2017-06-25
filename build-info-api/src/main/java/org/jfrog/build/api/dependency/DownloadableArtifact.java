package org.jfrog.build.api.dependency;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.dependency.pattern.PatternType;

import java.io.Serializable;

/**
 * Represents an artifact to be downloaded, used by generic resolving.
 *
 * @author Shay Yaakov
 */
public class DownloadableArtifact implements Serializable {
    String repoUrl;
    String targetDirPath;
    String relativeDirPath;
    String filePath;
    String matrixParameters;
    private PatternType patternType;
    private String sourcePattern;
    private boolean explode;

    public DownloadableArtifact() {
    }

    public DownloadableArtifact(String repoUrl, String targetDirPath, String filePath, String matrixParameters,
            String sourcePattern, PatternType patternType) {
        this.repoUrl = repoUrl;
        this.targetDirPath = targetDirPath == null ? "" : targetDirPath;
        this.filePath = filePath;
        this.matrixParameters = matrixParameters;
        this.patternType = patternType;
        this.sourcePattern = extractRepoFromPattern(sourcePattern);
        this.relativeDirPath = calculateRelativeDirFromPattern();
    }

    private String extractRepoFromPattern(String sourcePattern) {
        int indexOfColon = StringUtils.indexOf(sourcePattern, ":");
        if (indexOfColon == -1) {
            return sourcePattern;
        }

        return StringUtils.substring(sourcePattern, indexOfColon + 1, StringUtils.length(sourcePattern));
    }

    private String calculateRelativeDirFromPattern() {
        int firstStar = sourcePattern.indexOf('*');
        if (firstStar > 1) {
            String rootDirToRemove = sourcePattern.substring(0, firstStar);
            int lastSlash = rootDirToRemove.lastIndexOf('/');
            if (lastSlash > 1) {
                rootDirToRemove = rootDirToRemove.substring(0, lastSlash + 1);
                if (filePath.startsWith(rootDirToRemove)) {
                    return filePath.substring(rootDirToRemove.length());
                }
            }
        }
        return filePath;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public String getTargetDirPath() {
        return targetDirPath;
    }

    public void setTargetDirPath(String targetDirPath) {
         this.targetDirPath = targetDirPath;
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

    public String getMatrixParameters() {
        return matrixParameters;
    }

    public PatternType getPatternType() {
        return patternType;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public boolean isExplode() {
        return explode;
    }

    public void setExplode(boolean explode) {
        this.explode = explode;
    }
}
