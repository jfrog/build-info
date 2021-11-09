package org.jfrog.build.util;

import org.apache.tools.ant.Project;
import org.jfrog.build.api.util.Log;

/**
 * @author Noam Y. Tenne
 */
public class IvyBuildInfoLog implements Log {
    private Project project;

    public IvyBuildInfoLog(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void debug(String message) {
        project.log(message, Project.MSG_DEBUG);
    }

    public void info(String message) {
        project.log(message, Project.MSG_INFO);
    }

    public void warn(String message) {
        project.log(message, Project.MSG_WARN);
    }

    public void error(String message) {
        project.log(message, Project.MSG_ERR);
    }

    public void error(String message, Throwable e) {
        project.log(message, e, Project.MSG_ERR);
    }
}
