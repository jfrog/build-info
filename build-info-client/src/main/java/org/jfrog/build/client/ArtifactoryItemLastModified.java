package org.jfrog.build.client;

/**
 * @author Alexei Vainshtein
 */
public class ArtifactoryItemLastModified {
    public static final ArtifactoryItemLastModified NOT_FOUND = new ArtifactoryItemLastModified("", "");
    String uri;
    String lastModified;

    public ArtifactoryItemLastModified(String uri, String lastModified) {
        this.uri = uri;
        this.lastModified = lastModified;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    @Override
    public String toString() {
        return "uri: " + uri + "\n" + "lastModified:" + lastModified;
    }
}