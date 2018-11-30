package org.jfrog.build.extractor.npm.types;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
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
