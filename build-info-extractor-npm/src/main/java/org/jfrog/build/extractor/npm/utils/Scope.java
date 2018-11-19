package org.jfrog.build.extractor.npm.utils;

public enum Scope {
    PRODUCTION("production"),
    DEVELOPMENT("development");

    String scope;

    Scope(String scope) {
        this.scope = scope;
    }

    @Override
    public String toString() {
        return scope;
    }
}
