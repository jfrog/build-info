package org.jfrog.build.extractor.maven.transformer;

/**
 * This exception is thrown when a snapshot version is detected inside a pom when working in release mode.
 *
 * @author Yossi Shaul
 */
public class SnapshotNotAllowedException extends RuntimeException {

    public SnapshotNotAllowedException(String message) {
        super(message);
    }
}
