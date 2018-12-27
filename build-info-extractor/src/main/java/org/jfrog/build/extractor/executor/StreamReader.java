package org.jfrog.build.extractor.executor;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author Yahav Itzhak
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
            throw new RuntimeException(e);
        }
    }

    String getOutput() {
        return this.output;
    }
}