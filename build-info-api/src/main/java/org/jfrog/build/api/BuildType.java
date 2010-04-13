package org.jfrog.build.api;

/**
 * Defines the different types of builds
 *
 * @author Noam Y. Tenne
 * @deprecated Use {@link org.jfrog.build.api.BuildAgent} instead.
 */
@Deprecated
public enum BuildType {
    GENERIC("Generic"), MAVEN("Maven"), ANT("Ant"), IVY("Ivy"), GRADLE("Gradle");

    private String name;

    /**
     * Main constructor
     *
     * @param name Build type name
     */
    BuildType(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the build type
     *
     * @return Build type name
     */
    public String getName() {
        return name;
    }
}
