package org.jfrog.build.client;

import org.apache.http.Header;

/**
 * @author yahavi
 **/
@SuppressWarnings("unused")
public class DownloadResponse {
    public static final String SHA256_HEADER_NAME = "X-Checksum-Sha256";
    Header[] headers;
    String content;

    public DownloadResponse() {
    }

    public DownloadResponse(String content, Header[] headers) {
        this.headers = headers;
        this.content = content;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Header[] getHeaders() {
        return headers;
    }

    public String getContent() {
        return content;
    }
}
