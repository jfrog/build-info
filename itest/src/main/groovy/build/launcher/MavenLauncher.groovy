package build.launcher

import org.apache.tools.ant.DirectoryScanner
import org.apache.tools.ant.Project
import org.apache.tools.ant.types.FileSet

/**
 * @author Lior Hasson  
 */
class MavenLauncher extends Launcher{
    private def mavenHome
    private def javaHome

    MavenLauncher(String javaHome, String mavenHome, String projectFilePath) {
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
        cmd.add("${mavenHome}${File.separatorChar}bin${File.separatorChar}${mavenScript()} -v")
        cmd.add("$javaHome -classpath ${mavenHome}${File.separatorChar}boot$File.separatorChar${extractClassWorldJar()} ${systemPropsToString()} " +
                "org.codehaus.plexus.classworlds.launcher.Launcher -f $projectFilePath ${tasksToString()}"
        )
    }

    @Override
    protected def buildToolVersionHandler() {
        return null
    }

    private String extractClassWorldJar() {
        FileSet fs = new FileSet()
        fs.setDir(new File("$mavenHome${File.separatorChar}boot"))
        fs.setProject(new Project())
        fs.createInclude().setName("plexus-classworlds*.jar")
        DirectoryScanner ds = fs.getDirectoryScanner(new Project())
        String[] files = ds.getIncludedFiles()

        files[0]
    }

    private static def mavenScript(){
        (OS_WIN ? "mvn.bat" : "mvn")
    }
}
