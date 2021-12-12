package org.jfrog.build.api.dependency;

import java.io.Serializable;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class Pattern implements Serializable {
    private String pattern;

    public Pattern() {
    }

    public Pattern(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
}
