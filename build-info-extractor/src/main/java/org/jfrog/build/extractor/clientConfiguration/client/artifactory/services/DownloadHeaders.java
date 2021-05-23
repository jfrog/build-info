package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.Header;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class DownloadHeaders extends DownloadBase<Header[]> {
    public DownloadHeaders(String downloadFrom, Map<String, String> headers, Log log) {
        super(downloadFrom, true, headers, log);
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getHeaders();
    }

    protected void handleEmptyEntity() {
        result = getHeaders();
    }
}
