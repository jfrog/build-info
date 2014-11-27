package org.jfrog.build.api;

//import com.thoughtworks.xstream.annotations.XStreamAlias;

import java.io.Serializable;
//import static org.jfrog.build.api.BuildBean.RUN_PARAMETERS;

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
