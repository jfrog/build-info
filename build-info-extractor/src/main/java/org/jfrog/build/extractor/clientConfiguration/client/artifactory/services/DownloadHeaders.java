package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.Header;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;

import java.io.IOException;
import java.util.Map;

public class DownloadHeaders extends DownloadBase<Header[]> {
    public DownloadHeaders(String downloadFrom, Map<String, String> headers, Log log) {
        super(Header[].class, downloadFrom, true, headers, log);
    }

    @Override
    public Header[] execute(JFrogHttpClient client) throws IOException {
        super.execute(client);
        return getHeaders();
    }
}
