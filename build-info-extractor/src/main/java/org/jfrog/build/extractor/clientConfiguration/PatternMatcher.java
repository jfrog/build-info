package org.jfrog.build.extractor.clientConfiguration;

import org.apache.commons.lang3.StringUtils;

/**
 * Ant-style path pattern matcher
 *
 * @author Noam Y. Tenne
 */
public abstract class PatternMatcher {

    private PatternMatcher() {
    }

    /**
     * Indicates whether the given path conflicts with the given patterns.<br> A path will conflict if: The
     * include patterns list is not empty, and the path does not match any contained pattern. -Or-
     * The exclude patterns list is not empty, and the path does matches at-least one contained pattern.
     *
     * @param path     Path to check
     * @param patterns Patterns to match
     * @return True if the path conflicts
     */
    public static boolean pathConflicts(String path, IncludeExcludePatterns patterns) {
        if (!isPathIncluded(path, patterns)) {
            return true;
        }
        return isPathExcluded(path, patterns);
    }

    public static boolean isPathIncluded(String path, IncludeExcludePatterns patterns) {
        String[] includePatterns = patterns.getIncludePatterns();
        if (includePatterns.length == 0) {
            return true;
        }
        return pathMatchesPattern(path, includePatterns);
    }

    public static boolean isPathExcluded(String path, IncludeExcludePatterns patterns) {
        String[] excludePatterns = patterns.getExcludePatterns();
        if (excludePatterns.length == 0) {
            return false;
        }
        return pathMatchesPattern(path, excludePatterns);
    }

    /**
     * Indicates whether the give path matches the given patterns.<br/> A path will match if the pattern list is not
     * empty and if it matches to at least one pattern in the list.
     *
     * @param path     Path to check
     * @param patterns Patterns to match
     * @return True if the path any of the patterns.
     */
    private static boolean pathMatchesPattern(String path, String[] patterns) {
        for (String pattern : patterns) {
            if (StringUtils.isNotBlank(pattern) && match(pattern, path, false)) {
                return true;
            }
        }

        return false;
    }

    /**
     * THIS CODE WAS BORROWED FROM org.apache.tools.ant.types.selectors.SelectorUtils
     *
     * Tests whether or not a string matches against a pattern. The pattern may contain two special characters:<br> '*'
     * means zero or more characters<br> '?' means one and only one character
     *
     * @param pattern         The pattern to match against. Must not be <code>null</code>.
     * @param str             The string which must be matched against the pattern. Must not be <code>null</code>.
     * @param isCaseSensitive Whether or not matching should be performed case sensitively.
     * @return <code>true</code> if the string matches against the pattern, or <code>false</code> otherwise.
     */
    public static boolean match(String pattern, String str, boolean isCaseSensitive) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;

        boolean containsStar = false;
        for (int i = 0; i < patArr.length; i++) {
            if (patArr[i] == '*') {
                containsStar = true;
                break;
            }
        }

        if (!containsStar) {
            // No '*'s, so we make a shortcut
            if (patIdxEnd != strIdxEnd) {
                return false; // Pattern and string do not have the same size
            }
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?') {
                    if (isCaseSensitive && ch != strArr[i]) {
                        return false; // Character mismatch
                    }
                    if (!isCaseSensitive && Character.toUpperCase(ch)
                            != Character.toUpperCase(strArr[i])) {
                        return false;  // Character mismatch
                    }
                }
            }
            return true; // String matches against pattern
        }

        if (patIdxEnd == 0) {
            return true; // Pattern contains only '*', which matches anything
        }

        // Process characters before first star
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxStart]) {
                    return false; // Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch)
                        != Character.toUpperCase(strArr[strIdxStart])) {
                    return false; // Character mismatch
                }
            }
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // Process characters after last star
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?') {
                if (isCaseSensitive && ch != strArr[strIdxEnd]) {
                    return false; // Character mismatch
                }
                if (!isCaseSensitive && Character.toUpperCase(ch)
                        != Character.toUpperCase(strArr[strIdxEnd])) {
                    return false; // Character mismatch
                }
            }
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            // All characters in the string are used. Check if only '*'s are
            // left in the pattern. If so, we succeeded. Otherwise failure.
            for (int i = patIdxStart; i <= patIdxEnd; i++) {
                if (patArr[i] != '*') {
                    return false;
                }
            }
            return true;
        }

        // process pattern between stars. padIdxStart and patIdxEnd point
        // always to a '*'.
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                // Two stars next to each other, skip the first one.
                patIdxStart++;
                continue;
            }
            // Find the pattern between padIdxStart & padIdxTmp in str between
            // strIdxStart & strIdxEnd
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?') {
                        if (isCaseSensitive && ch != strArr[strIdxStart + i
                                + j]) {
                            continue strLoop;
                        }
                        if (!isCaseSensitive
                                && Character.toUpperCase(ch)
                                != Character.toUpperCase(strArr[strIdxStart + i + j])) {
                            continue strLoop;
                        }
                    }
                }

                foundIdx = strIdxStart + i;
                break;
            }

            if (foundIdx == -1) {
                return false;
            }

            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        // All characters in the string are used. Check if only '*'s are left
        // in the pattern. If so, we succeeded. Otherwise failure.
        for (int i = patIdxStart; i <= patIdxEnd; i++) {
            if (patArr[i] != '*') {
                return false;
            }
        }
        return true;
    }
}