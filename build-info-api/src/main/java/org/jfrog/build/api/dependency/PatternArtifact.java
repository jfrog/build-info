package org.jfrog.build.api.dependency;

import java.io.Serializable;

/**
 * Represents artifact matched by requested pattern. Part of {@link PatternResult}.
 *
 * @author jbaruch
 * @see BuildPatternArtifacts
 * @since 16/02/12
 */
public class PatternArtifact implements Serializable {

    private String uri;
    private String artifactoryUrl;
    private long size;
    private String lastModified;
    private String sha1;

    public PatternArtifact() {
    }

    public PatternArtifact(String artifactoryUrl, String uri, long size, String lastModified, String sha1) {
        this.artifactoryUrl = artifactoryUrl;
        this.uri = uri;
        this.size = size;
        this.lastModified = lastModified;
        this.sha1 = sha1;
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
}
