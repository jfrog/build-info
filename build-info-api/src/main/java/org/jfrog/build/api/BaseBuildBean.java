package org.jfrog.build.api;

import java.util.Properties;

/**
 * The main implementation of all the Build-API beans
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildBean implements BuildBean {

    private Properties properties;

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}