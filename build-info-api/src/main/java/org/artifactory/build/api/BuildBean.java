package org.artifactory.build.api;

import java.io.Serializable;

/**
 * The main interface of all the Build-API beans
 *
 * @author Noam Y. Tenne
 */
public interface BuildBean extends Serializable {

    public static final String ROOT = "build";
    public static final String MODULES = "modules";
    public static final String MODULE = "module";
    public static final String ARTIFACTS = "artifacts";
    public static final String ARTIFACT = "artifact";
    public static final String DEPENDENCIES = "dependencies";
    public static final String DEPENDENCY = "dependency";
}