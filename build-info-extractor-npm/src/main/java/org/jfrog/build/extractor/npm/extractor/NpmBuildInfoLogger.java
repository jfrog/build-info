package org.jfrog.build.extractor.npm.extractor;

import org.jfrog.build.api.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log npm install build outputs. Used only in Jenkins.
 *
 * @author yahavi
 */
public class NpmBuildInfoLogger implements Log {
    private Logger logger;

    public NpmBuildInfoLogger() {
        logger = Logger.getLogger(NpmBuildInfoLogger.class.getName());
    }

    public void debug(String message) {
        logger.fine(message);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warning(message);
    }

    public void error(String message) {
        logger.severe(message);
    }

    public void error(String message, Throwable e) {
        logger.log(Level.SEVERE, message, e);
    }
}
