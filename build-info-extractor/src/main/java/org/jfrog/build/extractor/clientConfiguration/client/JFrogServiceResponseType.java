package org.jfrog.build.extractor.clientConfiguration.client;

/**
 * Client response may contain a body, this enum indicates whenever a JFrog service expects a body or not.
 * EMPTY - An empty body is expected.
 * OBJECT - An object inside the body is expected.
 */
public enum JFrogServiceResponseType {
    EMPTY,
    OBJECT
}