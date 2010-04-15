package org.jfrog.build.api;

import java.io.Serializable;
import java.util.Properties;

/**
 * The main interface of all the Build-API beans
 *
 * @author Noam Y. Tenne
 */
public interface BuildBean extends Serializable {
    String ROOT = "build";
    String MODULES = "modules";
    String MODULE = "module";
    String ARTIFACTS = "artifacts";
    String ARTIFACT = "artifact";
    String DEPENDENCIES = "dependencies";
    String DEPENDENCY = "dependency";

    /**
     * Returns the properties of the bean
     *
     * @return Bean properties
     */
    Properties getProperties();

    /**
     * Sets the properties of the bean
     *
     * @param properties Bean properties
     */
    void setProperties(Properties properties);
}