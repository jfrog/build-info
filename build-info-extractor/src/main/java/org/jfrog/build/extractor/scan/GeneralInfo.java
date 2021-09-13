package org.jfrog.build.extractor.scan;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author yahavi
 */
public class GeneralInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String componentId = "";
    private String name = "";
    private String path = "";
    private String pkgType = "";
    private String groupId = "";
    private String artifactId = "";
    private String version = "";
    private String sha1 = "";
    private String prefix = "";

    @SuppressWarnings("WeakerAccess")
    public GeneralInfo() {
    }

    @SuppressWarnings("unused")
    public GeneralInfo(String componentId, String name, String path, String pkgType) {
        this.componentId = componentId;
        this.name = name;
        this.path = path;
        this.pkgType = pkgType;
    }

    public String getGroupId() {
        if (StringUtils.isNotBlank(groupId)) {
            return groupId;
        }
        return isValid() ? componentId.substring(0, componentId.indexOf(":")) : "";
    }

    public String getArtifactId() {
        if (StringUtils.isNotBlank(artifactId)) {
            return artifactId;
        }
        if (!isValid()) {
            return "";
        }
        int indexOfColon = componentId.indexOf(":");
        if (StringUtils.countMatches(componentId, ":") == 1) {
            return componentId.substring(0, indexOfColon);
        }
        return componentId.substring(indexOfColon + 1, componentId.lastIndexOf(":"));
    }

    public String getVersion() {
        if (StringUtils.isNotBlank(version)) {
            return version;
        }
        return isValid() ? componentId.substring(componentId.lastIndexOf(":") + 1) : "";
    }

    @SuppressWarnings("unused")
    public String getComponentId() {
        return componentId;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public String getPkgType() {
        return pkgType;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getSha1() {
        return sha1;
    }

    @SuppressWarnings("unused")
    public GeneralInfo componentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public GeneralInfo name(String name) {
        this.name = name;
        return this;
    }

    public GeneralInfo path(String path) {
        this.path = path;
        return this;
    }

    public GeneralInfo groupId(String groupId) {
        this.groupId = groupId;
        return this;
    }

    public GeneralInfo artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    public GeneralInfo version(String version) {
        this.version = version;
        return this;
    }

    @SuppressWarnings("unused")
    public GeneralInfo pkgType(String pkgType) {
        this.pkgType = pkgType;
        return this;
    }

    @SuppressWarnings("unused")
    public GeneralInfo prefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    public GeneralInfo sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }

    private boolean isValid() {
        int colonCount = StringUtils.countMatches(componentId, ":");
        return colonCount == 1 || colonCount == 2;
    }
}