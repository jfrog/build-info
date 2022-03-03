package org.jfrog.build.extractor;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.util.URI;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.codec.binary.StringUtils.getBytesUtf8;
import static org.apache.commons.codec.binary.StringUtils.newStringUsAscii;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * Created by Bar Belity on 19/07/2020.
 */
public class UrlUtils {

    private static final Pattern CREDENTIALS_IN_URL_REGEX = Pattern.compile("(http|https|git)://.+@");
    private static final String BUILD_PROJECT_PARAM = "?project=";

    /**
     * Remove URL credentials information from a given line (log, URL...).
     * The credentials are presented as 'user:password' or a token.
     *
     * @param lineWithCredentials - Line for masking URL with credentials.
     * @return The line with masked credentials.
     */
    public static String removeCredentialsFromUrl(String lineWithCredentials) {
        Matcher matcher = CREDENTIALS_IN_URL_REGEX.matcher(lineWithCredentials);
        if (!matcher.find()) {
            return lineWithCredentials;
        }
        String credentialsPart = matcher.group();
        String protocol = matcher.group(1);
        return StringUtils.replace(lineWithCredentials, credentialsPart, protocol + "://");
    }

    public static void appendParamsToUrl(Map<String, String> requestParams, StringBuilder urlBuilder) {
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

    /**
     * Get the project REST query parameter or an empty string, if project is blank.
     *
     * @param project - The project key
     * @param prefix  - The query param prefix
     * @return the project REST query parameter or an empty string.
     */
    public static String getProjectQueryParam(String project, String prefix) {
        return isEmpty(project) ? "" : prefix + encodeUrl(project);
    }

    /**
     * Get the project REST query parameter or an empty string, if project is blank.
     *
     * @param project - The project key
     * @return the project REST query parameter or an empty string.
     */
    public static String getProjectQueryParam(String project) {
        return getProjectQueryParam(project, BUILD_PROJECT_PARAM);
    }

    /**
     * Encode the URL path part. This function does encode slashes.
     * Use it for encoding a single path part in a URL - for example, encoding build name.
     *
     * @param pathPart - The URL path part to encode
     * @return the encoded URL path part.
     */
    public static String encodeUrlPathPart(String pathPart) {
        byte[] rawData = URLCodec.encodeUrl(null, getBytesUtf8(pathPart));
        return newStringUsAscii(rawData);
    }

    /**
     * Encode URL or the query part of a URL. This function does not encode slashes.
     * Use it for encoding multiple URL parts (a/b/c) or query params (?a=b).
     *
     * @param url - The URL to encode
     * @return the encoded URL.
     */
    public static String encodeUrl(String url) {
        byte[] rawData = URLCodec.encodeUrl(URI.allowed_query, getBytesUtf8(url));
        return newStringUsAscii(rawData);
    }
}
