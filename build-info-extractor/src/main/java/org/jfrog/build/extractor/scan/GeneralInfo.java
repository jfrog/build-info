package org.jfrog.build.extractor.scan;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

/**
 * @author yahavi
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneralInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String componentId = "";
    private String pkgType = "";
    private String path = "";
    private String sha1 = "";

    @SuppressWarnings("WeakerAccess")
    public GeneralInfo() {
    }

    @SuppressWarnings("unused")
    public GeneralInfo(String componentId, String path, String pkgType) {
        this.componentId = componentId;
        this.path = path;
        this.pkgType = pkgType;
    }

    public String getGroupId() {
        int colonMatches = StringUtils.countMatches(componentId, ":");
        if (colonMatches != 2) {
            return "";
        }
        return componentId.substring(0, componentId.indexOf(":"));
    }

    public String getArtifactId() {
        int colonMatches = StringUtils.countMatches(componentId, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        int indexOfColon = componentId.indexOf(":");
        if (colonMatches == 1) {
            return componentId.substring(0, indexOfColon);
        }
        return componentId.substring(indexOfColon + 1, componentId.lastIndexOf(":"));
    }

    public String getVersion() {
        int colonMatches = StringUtils.countMatches(componentId, ":");
        if (colonMatches < 1 || colonMatches > 2) {
            return "";
        }
        return componentId.substring(componentId.lastIndexOf(":") + 1);
    }

    @SuppressWarnings("unused")
    public String getComponentId() {
        return componentId;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("unused")
    public String getPkgType() {
        return pkgType;
    }

    public String getSha1() {
        return sha1;
    }

    @SuppressWarnings("unused")
    public GeneralInfo componentId(String componentId) {
        this.componentId = componentId;
        return this;
    }

    public GeneralInfo path(String path) {
        this.path = path;
        return this;
    }

    @SuppressWarnings("unused")
    public GeneralInfo pkgType(String pkgType) {
        this.pkgType = pkgType;
        return this;
    }

    public GeneralInfo sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }
}