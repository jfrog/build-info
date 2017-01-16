package org.jfrog.build.client.artifactoryXrayResponse;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;

/**
 * Used for serialization of Xray scanning results
 */
public class ImpactedArtifact implements Serializable {
    private final static long serialVersionUID = 1617312984328401853L;
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
    @JsonProperty("infected_files")
    private List<InfectedFile> infectedFiles;

    /**
     * No args constructor for use in serialization
     */
    public ImpactedArtifact() {
    }

    public ImpactedArtifact(String name, String displayName, String path, String pkgType, String sha256, String sha1, int depth, String parentSha, List<InfectedFile> infectedFiles) {
        this.name = name;
        this.displayName = displayName;
        this.path = path;
        this.pkgType = pkgType;
        this.sha256 = sha256;
        this.sha1 = sha1;
        this.depth = depth;
        this.parentSha = parentSha;
        this.infectedFiles = infectedFiles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("display_name")
    public String getDisplayName() {
        return displayName;
    }

    @JsonProperty("display_name")
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @JsonProperty("pkg_type")
    public String getPkgType() {
        return pkgType;
    }

    @JsonProperty("pkg_type")
    public void setPkgType(String pkgType) {
        this.pkgType = pkgType;
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

    @JsonProperty("infected_files")
    public List<InfectedFile> getInfectedFiles() {
        return infectedFiles;
    }

    @JsonProperty("infected_files")
    public void setInfectedFiles(List<InfectedFile> infectedFiles) {
        this.infectedFiles = infectedFiles;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
