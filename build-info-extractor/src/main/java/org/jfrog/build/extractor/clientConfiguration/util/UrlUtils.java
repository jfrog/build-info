package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jfrog.build.extractor.clientConfiguration.client.JFrogService.encodeUrl;

/**
 * Created by Bar Belity on 19/07/2020.
 */
public class UrlUtils {

    private static final Pattern CREDENTIALS_IN_URL_REGEX = Pattern.compile("(http|https|git)://.+@");

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
}
