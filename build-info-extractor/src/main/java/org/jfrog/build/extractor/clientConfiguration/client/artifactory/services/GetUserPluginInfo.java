package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetUserPluginInfo extends JFrogService<Map<String, List<Map>>> {
    public static final String USER_PLUGIN_ENDPOINT = "api/storage/";

    @SuppressWarnings("unchecked")
    public GetUserPluginInfo(Log log) {
        super((Class<Map<String, List<Map>>>) (Class<?>) Map.class, log);
        result = new HashMap<>();
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        return new HttpGet(USER_PLUGIN_ENDPOINT);
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        result = getMapper(true).readValue(stream, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, List.class));
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to obtain user plugin information.");
        throwException(response);
    }
}
