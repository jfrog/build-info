package org.jfrog.build.extractor.npm.types;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Dependency implements Serializable {
    private String name;
    private String version;
    private Set<String> scopes;
    private String artifactName;
    private String md5;
    private String sha1;

    public Dependency() {
    }

    public Dependency(String name, String version, String scope) {
        this.name = name;
        this.version = version;
        this.scopes = new HashSet<>();
        this.scopes.add(scope);
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

    public void setScope(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void addScope(String scope) {
        this.scopes.add(scope);
    }

    public String getArtifactName() {
        return artifactName;
    }

    public void setArtifactName(String artifactName) {
        this.artifactName = artifactName;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
}
