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

/**
 * JFrogService represents a generic way of processing a REST endpoint process that structures how REST sends, handles errors, and parses the response.
 *
 * @param <TResult> - The expected result class  from the JFrog REST endpoint.
 */
public abstract class JFrogService<TResult> {
    protected final Log log;
    protected TResult result;
    protected int statusCode;
    protected JFrogServiceResponseType responseType;
    private Header[] headers;
    private ObjectMapper mapper;

    protected JFrogService(Log log) {
        this.log = log;
        responseType = JFrogServiceResponseType.OBJECT;
    }

    protected JFrogService(Log log, JFrogServiceResponseType responseType) {
        this.log = log;
        this.responseType = responseType;
    }

    protected static void throwException(HttpEntity entity, int statusCode) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("JFrog service failed. Received " + statusCode);
        }
        try (InputStream stream = entity.getContent()) {
            String ResponseMessage = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
            throw new IOException("JFrog service failed. Received " + statusCode + ": " + ResponseMessage);
        }
    }

    public static String encodeUrl(String unescaped) {
        byte[] rawData = URLCodec.encodeUrl(URI.allowed_query, getBytesUtf8(unescaped));
        return newStringUsAscii(rawData);
    }

    /**
     * Default ObjectMapper to parse or deserialize JSON content into a Java object.
     */
    protected ObjectMapper getMapper() {
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
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

    /**
     * Override this in order to parse the service response body (only for services that expect to have body in the response).
     *
     * @param stream - Inout stream of the response body.
     */
    protected abstract void setResponse(InputStream stream) throws IOException;

    /**
     * Default error handling (E.G. 404 bad request).
     *
     * @param entity - The returned failure response.
     */
    protected void handleUnsuccessfulResponse(HttpEntity entity) throws IOException {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        throwException(entity, getStatusCode());
    }

    /**
     * Default execution of a service:
     * 1. Send the http request
     * 2. check HTTP status code. if there are any errors, throw.
     * 3. Return the response result (if any is expected).
     *
     * @param client - http client for sending the request.
     * @return - The response body object.
     */
    public TResult execute(JFrogHttpClient client) throws IOException {
        ensureRequirements(client);
        try (CloseableHttpResponse response = client.sendRequest(createRequest())) {
            if (response == null) {
                return null;
            }
            HttpEntity entity = response.getEntity();
            try {
                setStatusCode(response.getStatusLine().getStatusCode());
                setHeaders(response.getAllHeaders());
                if (getStatusCode() >= 400) {
                    handleUnsuccessfulResponse(entity);
                } else {
                    processResponse(entity);
                }
                return getResult();
            } finally {
                if (entity != null) {
                    EntityUtils.consumeQuietly(entity);
                }
            }
        }
    }

    private void processResponse(HttpEntity entity) throws IOException {
        if (responseType == JFrogServiceResponseType.EMPTY) {
            return;
        }
        if (entity == null) {
            handleEmptyEntity();
            return;
        }
        try (InputStream stream = entity.getContent()) {
            setResponse(stream);
        }
    }

    /**
     * For services with responseType.OBJECT (expected a return value) may
     * override this function which helps to handle scenarios whereby a response body needs to be read
     * but do entity is found.
     */
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
