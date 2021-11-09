package org.jfrog.build.api.dependency;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class PatternResultFileSet {

    private String repoUri;
    private String sourcePattern;
    private Set<String> files = new HashSet<>();

    public PatternResultFileSet() {
    }

    public PatternResultFileSet(String repoUri, String sourcePattern) {
        this.repoUri = repoUri;
        this.sourcePattern = sourcePattern;
    }

    public PatternResultFileSet(String repoUri, String sourcePattern, Set<String> files) {
        this.repoUri = repoUri;
        this.sourcePattern = sourcePattern;
        this.files = files;
    }

    public String getRepoUri() {
        return repoUri;
    }

    public void setRepoUri(String repoUri) {
        this.repoUri = repoUri;
    }

    public String getSourcePattern() {
        return sourcePattern;
    }

    public void setSourcePattern(String sourcePattern) {
        this.sourcePattern = sourcePattern;
    }

    public Set<String> getFiles() {
        return files;
    }

    public void setFiles(Set<String> files) {
        this.files = files;
    }

    public void addFile(String fileRelativePath) {
        if (files == null) {
            files = new HashSet<>();
        }

        files.add(fileRelativePath);
    }
}
