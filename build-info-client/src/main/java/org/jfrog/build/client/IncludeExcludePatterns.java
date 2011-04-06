package org.jfrog.build.client;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Holds include and exclude patterns to be used by the build server and extractor deployers
 *
 * @author Noam Y. Tenne
 */
public class IncludeExcludePatterns {
    //Empty array constant for the empty patterns
    private static final String[] EMPTY_PATTERN = new String[0];

    //Helper instance of empty patterns
    public static final IncludeExcludePatterns EMPTY = new IncludeExcludePatterns(EMPTY_PATTERN, EMPTY_PATTERN);

    private final String[] includePatterns;
    private final String[] excludePatterns;

    /**
     * Splitter constructor
     *
     * @param includePatterns Whitespace\comma-separated patterns. Can be null
     * @param excludePatterns Whitespace\comma-separated patterns. Can be null
     */
    public IncludeExcludePatterns(String includePatterns, String excludePatterns) {
        this.includePatterns = splitPatterns(includePatterns);
        this.excludePatterns = splitPatterns(excludePatterns);
    }

    /**
     * Pattern list constructor
     *
     * @param includePatterns Pattern array. Can be null
     * @param excludePatterns Pattern array. Can be null
     */
    public IncludeExcludePatterns(String[] includePatterns, String[] excludePatterns) {
        this.includePatterns = (includePatterns != null) ? includePatterns : EMPTY_PATTERN;
        this.excludePatterns = (excludePatterns != null) ? excludePatterns : EMPTY_PATTERN;
    }

    /**
     * Splits the given patterns string to an array
     *
     * @param patterns Whitespace\comma-separated patterns. Can be null
     * @return Pattern array
     */
    private String[] splitPatterns(String patterns) {
        if (StringUtils.isNotBlank(patterns)) {
            return StringUtils.split(patterns, ", ");
        } else {
            return EMPTY_PATTERN;
        }
    }

    public String[] getIncludePatterns() {
        return ((String[]) ArrayUtils.clone(includePatterns));
    }

    public String[] getExcludePatterns() {
        return ((String[]) ArrayUtils.clone(excludePatterns));
    }
}