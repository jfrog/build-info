package org.jfrog.build.launcher

import org.apache.tools.ant.DirectoryScanner
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet

/**
 * @author Lior Hasson  
 */
class MavenLauncher extends Launcher{
    private def mavenHome
    private def javaHome

    public MavenLauncher(String javaHome, String mavenHome, String projectFilePath) {
        super(projectFilePath)
        this.mavenHome = mavenHome
        this.javaHome = javaHome

        this.addSystemProp("maven.home", mavenHome)
    }

    String getJavaHome() {
        return javaHome
    }

    void setJavaHome(String javaHome) {
        this.javaHome = javaHome
    }

    String getMavenHome() {
        return mavenHome
    }

    void setMavenHome(String mavenHome) {
        this.mavenHome = mavenHome
    }

    @Override
    protected void createCmd() {
        extractClassWorldJar()

        cmd = "$javaHome -classpath ${mavenHome}\\boot\\${extractClassWorldJar()} ${systemPropsToString()} " +
                "org.codehaus.plexus.classworlds.launcher.Launcher -f $projectFilePath ${tasksToString()}"
    }

    private String extractClassWorldJar() {
        FileSet fs = new FileSet()
        fs.setDir(new File(mavenHome + "\\boot"))
        fs.setProject(new Project())
        fs.createInclude().setName("plexus-classworlds*.jar")
        DirectoryScanner ds = fs.getDirectoryScanner(new Project())
        String[] files = ds.getIncludedFiles()

        files[0];
    }
}
