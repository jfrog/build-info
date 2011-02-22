package org.jfrog.build.api;

/**
 * @author Noam Y. Tenne
 */
public enum StatusType {
    STAGED("Staged"), RELEASED("Released"), ROLLED_BACK("Rolled-back");

    private String displayName;

    StatusType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
