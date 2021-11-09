package org.jfrog.build.util;

/**
 * Special checked exception to hold relevant information of version resolution failures
 *
 * @author Noam Y. Tenne
 */
public class VersionException extends Exception {

    private VersionCompatibilityType versionCompatibilityType;

    public VersionException(String message, VersionCompatibilityType versionCompatibilityType) {
        super(message);
        this.versionCompatibilityType = versionCompatibilityType;
    }

    public VersionException(String message, Throwable cause, VersionCompatibilityType versionCompatibilityType) {
        super(message, cause);
        this.versionCompatibilityType = versionCompatibilityType;
    }

    public VersionCompatibilityType getVersionCompatibilityType() {
        return versionCompatibilityType;
    }
}
