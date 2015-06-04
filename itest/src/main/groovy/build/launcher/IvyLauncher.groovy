package build.launcher

/**
 * @author Lior Hasson  
 */
class IvyLauncher extends Launcher {

    IvyLauncher(String commandPath, String projectFilePath, String propertyFilePath) {
        super(commandPath, projectFilePath)
        processEnvironment.put("BUILDINFO_PROPFILE", propertyFilePath)
        processEnvironment.put("buildInfoConfig.propertiesFile", propertyFilePath)
    }

    @Override
    protected void createCmd() {
        cmd.add("${commandPath} ${systemPropsToString()} -file ${projectFilePath} ${tasksToString()} -lib " +
                "${this.getClass().getResource("/cache/artifactory-plugin").path} " +
                "-listener org.jfrog.build.extractor.listener.ArtifactoryBuildListener"
        )
    }

    @Override
    protected def buildToolVersionHandler() {
        //TODO
    }
}
