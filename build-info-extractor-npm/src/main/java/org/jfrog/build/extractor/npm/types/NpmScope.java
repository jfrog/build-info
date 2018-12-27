package org.jfrog.build.extractor.npm.types;

/**
 * @author Yahav Itzhak
 */
public enum NpmScope {
    PRODUCTION("production"),
    DEVELOPMENT("development");

    private String scope;

    NpmScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return scope;
    }
}
