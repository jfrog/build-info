package org.artifactory.build.api;

import java.io.Serializable;

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
}