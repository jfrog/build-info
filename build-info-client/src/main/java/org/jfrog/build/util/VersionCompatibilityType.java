package org.jfrog.build.util;

/**
 * Special enum to help distinguish between version resolution failures
 *
 * @author Noam Y. Tenne
 */
public enum VersionCompatibilityType {
    NOT_FOUND("Not Found"),
    INCOMPATIBLE("Incompatible");

    private String type;

    VersionCompatibilityType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
