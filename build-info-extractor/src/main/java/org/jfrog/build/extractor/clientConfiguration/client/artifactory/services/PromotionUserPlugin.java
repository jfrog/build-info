package org.jfrog.build.extractor.clientConfiguration.client.artifactory.services;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.VoidJFrogService;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;


public class PromotionUserPlugin extends VoidJFrogService {
    public static final String PROMOTION_USER_PLUGIN_ENDPOINT = "/api/plugins/build/promote/";

    private final String promotionName;
    private final String buildName;
    private final String buildNumber;
    private final Map<String, String> requestParams;

    public PromotionUserPlugin(String promotionName, String buildName, String buildNumber, Map<String, String> requestParams, Log logger) {
        super(logger);
        this.promotionName = promotionName;
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.requestParams = requestParams;
    }

    @Override
    public HttpRequestBase createRequest() throws IOException {
        StringBuilder urlBuilder = new StringBuilder(PROMOTION_USER_PLUGIN_ENDPOINT).append(promotionName).append("/").append(encodeUrl(buildName)).append("/")
                .append(encodeUrl(buildNumber)).append("?");
        appendParamsToUrl(requestParams, urlBuilder);
        return new HttpPost(urlBuilder.toString());
    }

    @Override
    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        log.error("Failed to promote user plugin.");
        throwException(response);
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
