package org.jfrog.build.client;

import org.apache.commons.lang.StringUtils;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryVersion {
    public static final ArtifactoryVersion NOT_FOUND = new ArtifactoryVersion("0.0.0");
    private static final String SNAPSHOT_SUFFIX = "SNAPSHOT";

    private final String version;
    private final String[] versionTokens;
    private boolean addons;

    public ArtifactoryVersion(String version) {
        this(version, false);
    }

    public ArtifactoryVersion(String version, boolean addons) {
        if (StringUtils.isBlank(version)) {
            throw new IllegalArgumentException("Please provide a valid version.");
        }
        this.version = version;
        this.addons = addons;
        versionTokens = StringUtils.split(version, ".");
    }

    public boolean isNotFound() {
        return NOT_FOUND.equals(this);
    }

    public boolean hasAddons() {
        return addons;
    }

    public boolean isAtLeast(ArtifactoryVersion atLeast) {
        if (atLeast != null) {
            for (int tokenIndex = 0; tokenIndex < atLeast.versionTokens.length; tokenIndex++) {
                String atLeastToken = atLeast.versionTokens[tokenIndex].trim();

                //If the current token index of atLeast is greater than this versions length, than at least is greater
                if (this.versionTokens.length < (tokenIndex + 1)) {
                    return false;
                }

                int comparison = compareTokens(this.versionTokens[tokenIndex].trim(), atLeastToken);

                //If the current token of this version is less than atLeasts current token
                if (comparison < 0) {
                    return false;
                }

                //If the current token of this version is greater than atLeasts current token
                if (comparison > 0) {
                    return true;
                }

                //If they are both equal, continue the comparison
            }
        }

        return true;
    }

    /**
     * @return less than 0 if toCheck is less than atLeast, 0 if they are equal or greater than 0 if toCheck is greater
     *         than atLeast
     */
    private int compareTokens(String toCheck, String atLeastToken) {
        boolean toCheckIsBlank = StringUtils.isBlank(toCheck);
        boolean atLeastTokenIsBlank = StringUtils.isBlank(atLeastToken);
        if (toCheckIsBlank && atLeastTokenIsBlank) {
            return 0;
        } else if (!toCheckIsBlank && atLeastTokenIsBlank) {
            return 1;
        } else if (toCheckIsBlank && !atLeastTokenIsBlank) {
            return -1;
        }

        if (StringUtils.isNumeric(atLeastToken)) {
            return compareToCheckToNumericAtLeast(toCheck, atLeastToken);
        } else if (StringUtils.isAlphanumeric(atLeastToken)) {
            String atLeastTokenFirstNumerals = getTokenFirstNumerals(atLeastToken);
            if (StringUtils.isNotBlank(atLeastTokenFirstNumerals)) {
                return compareToCheckToNumericAtLeast(toCheck, atLeastTokenFirstNumerals);
            } else {
                if (StringUtils.isNumeric(toCheck)) {
                    return -1;
                }
            }
        }

        int comparison = toCheck.compareTo(atLeastToken);
        if (comparison == 0) {
            boolean toCheckIsSnapshot = toCheck.contains(SNAPSHOT_SUFFIX);
            boolean atLeastIsSnapshot = atLeastToken.contains(SNAPSHOT_SUFFIX);
            if (toCheckIsSnapshot && !atLeastIsSnapshot) {
                return 1;
            } else if (!toCheckIsSnapshot && atLeastIsSnapshot) {
                return -1;
            }
        }

        return comparison;
    }

    /**
     * @return Less than 0 if toCheck is less than atLeast, 0 if they are equal and more than 0 if toCheck is greater
     *         than atLeast, is only or starts with alpha chars
     */
    private int compareToCheckToNumericAtLeast(String toCheck, String atLeast) {
        if (StringUtils.isNumeric(toCheck)) {
            return compareNumerals(toCheck, atLeast);
        } else if (StringUtils.isAlphanumeric(toCheck)) {
            return compareAlphaNumericToCheckToNumericAtLeast(toCheck, atLeast);
        }
        return 1;
    }

    /**
     * @return Less than 0 if toCheck is less than atLeast, 0 if they are equal and more than 0 if toCheck is greater
     *         than atLeast or starts with alpha chars
     */
    private int compareAlphaNumericToCheckToNumericAtLeast(String toCheck, String atLeast) {
        String toCheckFirstNumerals = getTokenFirstNumerals(toCheck);
        if (StringUtils.isBlank(toCheckFirstNumerals)) {
            return 1;
        }
        return compareNumerals(toCheckFirstNumerals, atLeast);
    }

    /**
     * @return Less than 0 if toCheck is less than atLeast, 0 if they are equal and more than 0 if toCheck is greater
     *         than atLeast
     */
    private int compareNumerals(String toCheck, String atLeast) {
        return (Integer.valueOf(toCheck).compareTo(Integer.valueOf(atLeast)));
    }

    private String getTokenFirstNumerals(String token) {
        char[] chars = token.toCharArray();
        StringBuilder numerals = new StringBuilder();
        for (char c : chars) {
            if (!Character.isDigit(chars[0])) {
                break;
            }
            numerals.append(c);
        }
        return numerals.toString();
    }

    @Override
    public String toString() {
        return version;
    }
}
