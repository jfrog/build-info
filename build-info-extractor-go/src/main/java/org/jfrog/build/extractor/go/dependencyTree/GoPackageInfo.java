package org.jfrog.build.extractor.go.dependencyTree;

import java.io.Serializable;
import java.util.Objects;

/**
 * Created by Bar Belity on 13/02/2020.
 */
public class GoPackageInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private String version;

    @SuppressWarnings("unused")
    public GoPackageInfo() {
    }

    public GoPackageInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    @SuppressWarnings("unused")
    public String getName() {
        return name;
    }

    @SuppressWarnings("unused")
    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GoPackageInfo)) return false;
        GoPackageInfo that = (GoPackageInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version);
    }

    @Override
    public String toString() {
        return version == null ? name : name + ":" + version;
    }
}
