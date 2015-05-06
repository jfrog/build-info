package org.jfrog.build.org.jfrog.build.lancher

/**
 * @author Lior Hasson  
 */
class MavenLauncher extends Launcher{
    public MavenLauncher(Object commandPath, Object projectFilePath) {
        super(commandPath, projectFilePath)
    }

    @Override
    protected void createCmd() {

    }
}
