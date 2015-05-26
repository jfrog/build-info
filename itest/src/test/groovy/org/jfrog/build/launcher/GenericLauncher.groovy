package org.jfrog.build.launcher

/**
 * @author Lior Hasson  
 */
class GenericLauncher extends Launcher{
    GenericLauncher(Object projectFilePath) {
        super(projectFilePath)
    }

    @Override
    protected void createCmd() {

    }

    @Override
    protected buildToolVersionHandler() {
        return null
    }
}
