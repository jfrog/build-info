package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DownloadToFile extends DownloadBase<File> {
    private final String downloadTo;

    public DownloadToFile(String downloadFrom, String downloadTo, Map<String, String> headers, Log log) {
        super(File.class, downloadFrom, false, headers, log);
        this.downloadTo = downloadTo;
    }

    private static File saveInputStreamToFile(InputStream inputStream, String filePath) throws IOException {
        // Create file
        File dest = new File(filePath);
        if (dest.exists()) {
            dest.delete();
        } else {
            dest.getParentFile().mkdirs();
        }

        // Save InputStream to file
        try (FileOutputStream fileOutputStream = new FileOutputStream(dest)) {
            IOUtils.copyLarge(inputStream, fileOutputStream);

            return dest;
        } catch (IOException e) {
            throw new IOException(String.format("Could not create or write to file: %s", dest.getCanonicalPath()), e);
        }
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        result = saveInputStreamToFile(stream, downloadTo);
    }
}
