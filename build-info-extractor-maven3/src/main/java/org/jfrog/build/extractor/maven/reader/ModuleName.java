package org.jfrog.build.extractor.maven.reader;

import java.io.Serializable;

/**
 * Container object that contains the groupId and artifactId for a Maven model.
 *
 * @author Tomer Cohen
 */
public class ModuleName implements Serializable {

    private final String groupId;

    private final String artifactId;

    public ModuleName(String groupId, String artifactId) {
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ModuleName name = (ModuleName) o;

        return artifactId.equals(name.artifactId) && groupId.equals(name.groupId);
    }

    @Override
    public int hashCode() {
        int result = groupId.hashCode();
        result = 31 * result + artifactId.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getGroupId()).append(":").append(getArtifactId());
        return sb.toString();
    }
}
