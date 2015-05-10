package org.jfrog.build.launcher

/**
 * @author Aviad Shikloshi
 */
class IvyLauncher extends Launcher {

    IvyLauncher(String commandPath, String projectFilePath) {
        super(commandPath, projectFilePath)
    }

    @Override
    protected void createCmd() {
        cmd = "${commandPath} -file ${projectFilePath} ${tasksToString()} ${systemPropsToString()}"
    }
}
