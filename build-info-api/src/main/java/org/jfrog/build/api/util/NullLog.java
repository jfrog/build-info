package org.jfrog.build.api.util;

/**
 * A log implementation that doesn't do anything
 *
 * @author Noam Y. Tenne
 */
public class NullLog implements Log {

    public void debug(String message) {
        //nop
    }

    public void info(String message) {
        //nop
    }

    public void warn(String message) {
        //nop
    }

    public void error(String message) {
        //nop
    }

    public void error(String message, Throwable e) {
        //nop
    }
}
