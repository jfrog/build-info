package org.jfrog.build.extractor.buildTool;

import org.jfrog.build.api.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Log build-tool execution outputs.
 * Since Jenkins is currently the only CI server which executes build-tools builds in a new process,
 * this logger is currently used by Jenkins only.
 *
 * @author yahavi
 */
public class BuildToolLogger implements Log {
    private Logger logger;

    public BuildToolLogger() {
        logger = Logger.getLogger(BuildToolLogger.class.getName());
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
