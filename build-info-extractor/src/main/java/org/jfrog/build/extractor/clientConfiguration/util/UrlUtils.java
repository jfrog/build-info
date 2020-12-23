package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
}
