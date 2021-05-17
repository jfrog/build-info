package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

public abstract class DownloadBase<TResult> extends JFrogService<TResult> {
    private final String DownloadPath;
    private final boolean isHead;
    private final Map<String, String> headers;

    protected DownloadBase(Class<TResult> resultClass, String DownloadPath, boolean isHead, Map<String, String> headers, Log log) {
        super(resultClass, log);
        this.DownloadPath = encodeUrl(DownloadPath);
        this.isHead = isHead;
        this.headers = headers;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpRequestBase request = isHead ? new HttpHead(DownloadPath) : new HttpGet(DownloadPath);
        // Explicitly force keep alive
        request.setHeader("Connection", "Keep-Alive");
        // Add all required headers to the request
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                request.setHeader(entry.getKey(), entry.getValue());
            }
        }
        return request;
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            throw new FileNotFoundException("Unable to find " + DownloadPath);
        }
        log.error("Failed to download from  '" + DownloadPath + "'");
        throwException(response);
    }
}
