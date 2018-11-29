package org.jfrog.build.extractor.executor;

import java.io.*;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class StreamReader implements Runnable {
    private String output;
    private InputStream inputStream;

    StreamReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public void run() {
        try {
            output = readStream(inputStream);
        } catch (IOException e) {
            output = e.toString();
        }
    }

    String getOutput() {
        return this.output;
    }

    private static String readStream(InputStream in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }
}