package org.jfrog.build.extractor.ci;

import java.util.Properties;

/**
 * The main implementation of all the BuildInfo-API beans
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