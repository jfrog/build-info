package org.artifactory.build.api;

import java.util.Properties;

/**
 * The main implementation of all the Build-API beans
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildBean implements BuildBean {

    private Properties properties;

    /**
     * Returns the properties of the bean
     *
     * @return Bean properties
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Sets the properties of the bean
     *
     * @param properties Bean properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}