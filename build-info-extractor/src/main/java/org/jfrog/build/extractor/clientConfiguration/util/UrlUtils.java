package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Bar Belity on 19/07/2020.
 */
public class UrlUtils {

    private static Pattern CredentialsInUrlRegexpPattern = Pattern.compile("((http|https):\\/\\/.+:.*@)");

    /**
     * Mask the credentials information from a given line (log, url...).
     * The credentials are presented as user:password.
     * @param lineWithCredentials - Line for masking url with credentials.
     * @return The line with masked credentials.
     */
    public static String maskCredentialsInUrl(String lineWithCredentials) {
        Matcher matcher = CredentialsInUrlRegexpPattern.matcher(lineWithCredentials);
        if (!matcher.find()) {
            return lineWithCredentials;
        }
        String credentialsPart = matcher.group();
        String[] split = credentialsPart.split("//");
        return StringUtils.replace(lineWithCredentials, credentialsPart, split[0] + "//***.***@", 1);
    }
}
