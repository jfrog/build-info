package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import static org.jfrog.build.extractor.UrlUtils.encodeUrl;

public abstract class DownloadBase<TResult> extends JFrogService<TResult> {
    private final String DownloadPath;
    private final boolean isHead;
    private final Map<String, String> headers;

    protected DownloadBase(String downloadPath, boolean isHead, Map<String, String> headers, Log log) {
        super(log);
        this.DownloadPath = encodeUrl(downloadPath);
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
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (statusCode == HttpStatus.SC_NOT_FOUND) {
            throw new FileNotFoundException("Unable to find " + DownloadPath);
        }
        log.error("Failed to download from  '" + DownloadPath + "'");
        throwException(entity, getStatusCode());
    }
}
