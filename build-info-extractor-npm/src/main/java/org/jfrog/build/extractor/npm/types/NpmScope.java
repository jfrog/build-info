package org.jfrog.build.extractor.npm.types;

public enum NpmScope {
    PRODUCTION("production"),
    DEVELOPMENT("development");

    String scope;

    NpmScope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return scope;
    }
}
