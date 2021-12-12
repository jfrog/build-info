package org.jfrog.build.api.util;

import java.io.Serializable;

/**
 * An interface that wraps a provided logger. Used to delegate logging to the runtime environment logger.
 *
 * @author Noam Y. Tenne
 */
public interface Log extends Serializable, org.jfrog.filespecs.utils.Log {
    void debug(String message);

    void info(String message);

    void warn(String message);

    void error(String message);

    void error(String message, Throwable e);
}