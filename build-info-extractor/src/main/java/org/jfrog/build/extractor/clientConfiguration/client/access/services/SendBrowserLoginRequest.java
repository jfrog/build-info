package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.BROWSER_LOGIN_ENDPOINT;
import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

public class SendBrowserLoginRequest extends VoidJFrogService {
    private final String uuid;

    public SendBrowserLoginRequest(String uuid, Log logger) {
        super(logger);
        this.uuid = uuid;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        HttpPost request = new HttpPost(BROWSER_LOGIN_ENDPOINT + "/request");
        BrowserLoginRequest browserLoginRequest = new BrowserLoginRequest(uuid);
        StringEntity stringEntity = new StringEntity(toJsonString(browserLoginRequest), ContentType.APPLICATION_JSON);
        request.setEntity(stringEntity);
        return request;
    }

    private static class BrowserLoginRequest {
        private final String session;

        public BrowserLoginRequest(String session) {
            this.session = session;
        }

        @SuppressWarnings("unused")
        public String getSession() {
            return session;
        }
    }
}
