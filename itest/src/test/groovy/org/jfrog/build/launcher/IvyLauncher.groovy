package org.jfrog.build.launcher

/**
 * @author Lior Hasson  
 */
class IvyLauncher extends Launcher {

    IvyLauncher(String commandPath, String projectFilePath, String propertyFilePath) {
        super(commandPath, projectFilePath)
        processEnvironment.put("BUILDINFO_PROPFILE", propertyFilePath)
    }

    @Override
    protected void createCmd() {
        cmd = "${commandPath} -file ${projectFilePath} ${tasksToString()} -lib " +
                "${this.getClass().getResource("/org/jfrog/build/cache/artifactory-plugin").path} " +
                "-listener org.jfrog.build.extractor.listener.ArtifactoryBuildListener"
    }
}
