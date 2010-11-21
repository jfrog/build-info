package org.jfrog.build.extractor.logger;

import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.Logger;
import org.jfrog.build.api.util.Log;

/**
 * Logger that is to be used for the HTTP client when using Gradle.
 *
 * @author Tomer Cohen
 * @see org.jfrog.build.client.ArtifactoryBuildInfoClient
 */
public class GradleClientLogger implements Log {

    private Logger logger;

    public GradleClientLogger(Logger logger) {
        this.logger = logger;
    }

    public void debug(String message) {
        logger.log(LogLevel.DEBUG, message);
    }

    public void info(String message) {
        logger.log(LogLevel.LIFECYCLE, message);
    }

    public void warn(String message) {
        logger.log(LogLevel.WARN, message);
    }

    public void error(String message) {
        logger.log(LogLevel.ERROR, message);
    }

    public void error(String message, Throwable e) {
        logger.log(LogLevel.ERROR, message, e);
    }
}
