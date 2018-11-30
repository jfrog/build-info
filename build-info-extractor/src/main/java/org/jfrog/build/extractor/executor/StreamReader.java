package org.jfrog.build.extractor.executor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class StreamReader implements Runnable {

    private InputStream inputStream;
    private String output;

    StreamReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try {
            output = IOUtils.toString(inputStream, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            output = ExceptionUtils.getStackTrace(e);
        }
    }

    String getOutput() {
        return this.output;
    }
}