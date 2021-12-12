

package org.jfrog.build.extractor.ci;

import java.io.Serializable;
import java.util.Properties;

/**
 * The main interface of all the Build-API beans
 */
public interface BuildBean extends Serializable {
    String ROOT = "build";
    String MODULES = "modules";
    String MODULE = "module";
    String ARTIFACTS = "artifacts";
    String EXCLUDED_ARTIFACTS = "excludedArtifacts";
    String ARTIFACT = "artifact";
    String DEPENDENCIES = "dependencies";
    String DEPENDENCY = "dependency";
    String RUN_PARAMETERS = "runParameters";

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