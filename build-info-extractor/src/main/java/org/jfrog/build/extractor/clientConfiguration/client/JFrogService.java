package org.jfrog.build.extractor.clientConfiguration.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.util.URI;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.binary.StringUtils.newStringUsAscii;

public abstract class JFrogService<TResult> {
    protected final Class<TResult> resultClass;
    protected final Log log;
    protected TResult result;
    protected int statusCode;
    protected JFrogServiceResponseType responseType;
    private Header[] headers;
    private ObjectMapper mapper;


    protected JFrogService(Class<TResult> resultClass, Log log) {
        this.resultClass = resultClass;
        this.log = log;
        responseType = JFrogServiceResponseType.OBJECT;
    }

    protected JFrogService(Class<TResult> resultClass, Log log, JFrogServiceResponseType responseType) {
        this.resultClass = resultClass;
        this.log = log;
        this.responseType = responseType;
    }

    protected static void throwException(CloseableHttpResponse response) throws IOException {
        if (response == null) {
            throw new IllegalArgumentException("Response cannot be null");
        }
        try (InputStream stream = response.getEntity().getContent()) {
            String ResponseMessage = IOUtils.toString(stream, StandardCharsets.UTF_8);
            throw new IOException("JFrog service failed. Received " + response.getStatusLine().getStatusCode() + ": " + ResponseMessage);
        } finally {
            EntityUtils.consumeQuietly(response.getEntity());
        }

    }

    public static String encodeUrl(String unescaped) {
        byte[] rawData = URLCodec.encodeUrl(URI.allowed_query, getBytesUtf8(unescaped));
        return newStringUsAscii(rawData);
    }

    protected ObjectMapper getMapper(boolean ignoreMissingFields) {
        if (mapper == null) {
            mapper = new ObjectMapper();
            if (ignoreMissingFields) {
                mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            }
            mapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }
        return mapper;
    }

    public TResult getResult() {
        return result;
    }

    public void setResult(TResult result) {
        this.result = result;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public abstract HttpRequestBase createRequest() throws IOException;

    public void setResponse(InputStream stream) throws IOException {
        throw new UnsupportedOperationException("The service '" + getClass().getSimpleName() + "' must override the setResponse method");
    }

    protected void handleUnsuccessfulResponse(CloseableHttpResponse response) throws IOException {
        throwException(response);
    }

    public TResult execute(JFrogHttpClient client) throws IOException {
        ensureRequirements(client);
        try (CloseableHttpResponse response = client.sendRequest(createRequest())) {
            if (response == null) {
                return null;
            }
            setStatusCode(response.getStatusLine().getStatusCode());
            setHeaders(response.getAllHeaders());
            if (getStatusCode() >= 400) {
                handleUnsuccessfulResponse(response);
            } else {
                processResponse(response);
            }
            return getResult();
        }
    }

    public void processResponse(CloseableHttpResponse response) throws IOException {
        HttpEntity entity = response.getEntity();
        try {
            if (entity == null) {
                handleEmptyEntity();
                return;
            }
            try (InputStream stream = entity.getContent()) {
                long contentLength = entity.getContentLength();
                if (contentLength == 0 || responseType == JFrogServiceResponseType.EMPTY) {
                    return;
                }
                setResponse(stream);
            }
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    protected void handleEmptyEntity() throws IOException {
    }

    protected void ensureRequirements(JFrogHttpClient client) throws IOException {
    }

    public Header[] getHeaders() {
        return headers;
    }

    public void setHeaders(Header[] headers) {
        this.headers = headers;
    }
}
