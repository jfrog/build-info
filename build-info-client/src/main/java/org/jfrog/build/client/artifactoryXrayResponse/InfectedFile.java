package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;

/**
 * Used for serialization of Xray scanning results
 */
public class InfectedFile implements Serializable {
    private final static long serialVersionUID = -8372191649914079447L;
    private String name;
    private String path;
    private String sha256;
    private String sha1;
    private int depth;
    @JsonProperty("pkg_type")
    private String pkgType;
    @JsonProperty("parent_sha")
    private String parentSha;
    @JsonProperty("display_name")
    private String displayName;

    /**
     * No args constructor for use in serialization
     */
    public InfectedFile() {
    }

    public InfectedFile(String name, String path, String sha256, String sha1, int depth, String parentSha, String displayName, String pkgType) {
        this.name = name;
        this.path = path;
        this.sha256 = sha256;
        this.sha1 = sha1;
        this.depth = depth;
        this.parentSha = parentSha;
        this.displayName = displayName;
        this.pkgType = pkgType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSha256() {
        return sha256;
    }

    public void setSha256(String sha256) {
        this.sha256 = sha256;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    @JsonProperty("parent_sha")
    public String getParentSha() {
        return parentSha;
    }

    @JsonProperty("parent_sha")
    public void setParentSha(String parentSha) {
        this.parentSha = parentSha;
    }

    @JsonProperty("display_name")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("display_name")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @JsonProperty("pkg_type")
    public String getPkgType() {
        return pkgType;
    }

    @JsonProperty("pkg_type")
    public void setPkgType(String pkgType) {
        this.pkgType = pkgType;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
