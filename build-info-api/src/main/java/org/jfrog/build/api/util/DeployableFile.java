package org.jfrog.build.api.util;

import org.jfrog.build.api.BaseBuildFileBean;
import org.jfrog.build.api.BuildFileBean;

import java.io.File;

/**
 * Contains data about a build file (an artifact or a dependency) with its physical file information
 *
 * @author Noam Y. Tenne
 */
public class DeployableFile extends BaseBuildFileBean {

    /**
     * The build file details
     */
    private BuildFileBean buildFile;

    /**
     * The file to deploy
     */
    File file;

    public DeployableFile(BuildFileBean buildFile, File file) {
        this.buildFile = buildFile;
        this.file = file;
    }

    public BuildFileBean getBuildFile() {
        return buildFile;
    }

    public File getFile() {
        return file;
    }
}