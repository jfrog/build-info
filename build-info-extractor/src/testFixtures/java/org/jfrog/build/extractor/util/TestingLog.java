package org.jfrog.build.extractor.util;

import org.jfrog.build.api.util.Log;
import org.testng.Reporter;

public class TestingLog implements Log {
    @Override
    public void debug(String message) {
        Reporter.log(message, 2, true);
    }

    @Override
    public void info(String message) {
        Reporter.log(message, 1, true);
    }

    @Override
    public void warn(String message) {
        Reporter.log(message, 1, true);
    }

    @Override
    public void error(String message) {
        Reporter.log(message, 0, true);
    }

    @Override
    public void error(String message, Throwable e) {
        Reporter.log(message, 0, true);
    }
}
