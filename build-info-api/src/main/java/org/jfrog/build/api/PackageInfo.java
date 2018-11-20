package org.jfrog.build.api;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class PackageInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String version;
    private Set<String> scopes = new HashSet<>();

    @SuppressWarnings("unused")
    public PackageInfo() {
    }

    public PackageInfo(String name, String version, String scopes) {
        this.name = name;
        this.version = version;
        this.scopes.add(scopes);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj.getClass() != getClass()) {
            return false;
        }
        PackageInfo other = (PackageInfo) obj;
        return Objects.equals(name, other.getName()) && Objects.equals(version, other.getVersion()) && Objects.equals(scopes, other.getScopes());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, scopes);
    }

    @Override
    public String toString() {
        return name + ":" + version;
    }
}
