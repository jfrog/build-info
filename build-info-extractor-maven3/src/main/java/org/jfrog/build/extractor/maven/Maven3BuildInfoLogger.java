package org.jfrog.build.extractor.maven;

import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.util.Log;

/**
 * @author Noam Y. Tenne
 */
public class Maven3BuildInfoLogger implements Log {
    private Logger logger;

    public Maven3BuildInfoLogger(Logger logger) {
        this.logger = logger;
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable e) {
        logger.error(message, e);
    }
}
