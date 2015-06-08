package build.generic

import org.jfrog.build.api.util.Log
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Lior Hasson  
 */
class BuildInfoLog implements Log{
    Logger logger;

    BuildInfoLog(Class clazz) {
        logger = LoggerFactory.getLogger(clazz)
    }

    @Override
    void debug(String message) {
        logger.debug(message)
    }

    @Override
    void info(String message) {
        logger.info(message)
    }

    @Override
    void warn(String message) {
        logger.warn(message)
    }

    @Override
    void error(String message) {
        logger.error(message)
    }

    @Override
    void error(String message, Throwable e) {
        logger.error(message, e)
    }
}
