package org.jfrog.build.api.ci;


import java.io.Serializable;

/**
 * @author Lior Hasson
 */

public class MatrixParameter implements Serializable {

    private String key;
    private String value;

    /**
     * Default constructor
     */
    public MatrixParameter() {
    }

    /**
     * Main constructor
     *
     * @param key   Agent name
     * @param value Agent version
     */
    public MatrixParameter(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
