package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.JFrogService;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GetStagingStrategy extends JFrogService<Map> {
    private static final String BUILD_STAGING_STRATEGY_ENDPOINT = "api/plugins/build/staging/";
    private final String strategyName;
    private final String buildName;
    private final Map<String, String> requestParams;

    public GetStagingStrategy(String strategyName, String buildName, Map<String, String> requestParams, Log log) {
        super(Map.class, log);
        this.strategyName = strategyName;
        this.buildName = buildName;
        this.requestParams = requestParams;
        result = new HashMap<>();
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(BUILD_STAGING_STRATEGY_ENDPOINT)
                .append(encodeUrl(strategyName)).append("?buildName=")
                .append(encodeUrl(buildName)).append("&");
        appendParamsToUrl(requestParams, urlBuilder);
        return new HttpGet(urlBuilder.toString());
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to obtain staging strategy.");
        throwException(response);
    }

    @Override
    public void setResponse(InputStream stream) throws IOException {
        result = getMapper(true).readValue(stream, resultClass);
    }

    private void appendParamsToUrl(Map<String, String> requestParams, StringBuilder urlBuilder) {
        if ((requestParams != null) && !requestParams.isEmpty()) {
            urlBuilder.append("params=");
            Iterator<Map.Entry<String, String>> paramEntryIterator = requestParams.entrySet().iterator();
            String encodedPipe = encodeUrl("|");
            while (paramEntryIterator.hasNext()) {
                Map.Entry<String, String> paramEntry = paramEntryIterator.next();
                urlBuilder.append(encodeUrl(paramEntry.getKey()));
                String paramValue = paramEntry.getValue();
                if (StringUtils.isNotBlank(paramValue)) {
                    urlBuilder.append("=").append(encodeUrl(paramValue));
                }

                if (paramEntryIterator.hasNext()) {

                    urlBuilder.append(encodedPipe);
                }
            }
        }
    }
}
