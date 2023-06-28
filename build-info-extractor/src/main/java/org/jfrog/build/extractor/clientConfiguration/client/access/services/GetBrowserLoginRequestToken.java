package org.jfrog.build.extractor.clientConfiguration.client.access.services;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;
import org.jfrog.build.extractor.clientConfiguration.client.response.CreateAccessTokenResponse;

import java.io.IOException;
import java.io.InputStream;

import static org.jfrog.build.extractor.clientConfiguration.client.access.services.Utils.BROWSER_LOGIN_ENDPOINT;

public class GetBrowserLoginRequestToken extends JFrogService<CreateAccessTokenResponse> {
    private final String uuid;

    public GetBrowserLoginRequestToken(String uuid, Log logger) {
        super(logger);
        this.uuid = uuid;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(String.format("%s/token/%s", BROWSER_LOGIN_ENDPOINT, uuid));
    }

    @Override
    protected void setResponse(InputStream stream) throws IOException {
        result = getMapper().readValue(stream, CreateAccessTokenResponse.class);
    }

    @Override
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        // Until the user successfully completes the login process, Access will return status code 400.
        if (getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            return;
        }
        super.handleUnsuccessfulResponse(entity);
    }
}
