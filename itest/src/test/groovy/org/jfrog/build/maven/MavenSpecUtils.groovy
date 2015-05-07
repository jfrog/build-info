package org.jfrog.build.maven

import org.jfrog.build.launcher.MavenLauncher

/**
 * @author Lior Hasson  
 */
class MavenSpecUtils {

//    def static getGradleCommandPath() {
//        System.getenv("GRADLE_HOME") + File.separator + "bin" + File.separator + "gradle.bat"
//    }

    private static def getMavenProjectFile() {
//        def resource = getClass().getResource("/org/jfrog/build/maven")
        new File("C:\\Users\\liorh\\.jenkins\\jobs\\mavenFreeStyleArfifactoryPlugin\\workspace\\maven-example\\pom.xml")
    }

    def static MavenLauncher createGradleLauncher() {
        File projectFile = getMavenProjectFile()
        MavenLauncher launcher = new MavenLauncher("java", "C:\\Software\\apache-maven-3.2.5", projectFile.getCanonicalPath())
                .addSystemProp("buildInfoConfig.propertiesFile", "C:\\Work\\buildInfo\\build-info\\build-info-extractor-maven3\\src\\test\\resources\\it\\buildInfo3251772824884199176.properties")
                .addSystemProp("m3plugin.lib", "C:\\Work\\buildInfo\\build-info\\test\\cache\\2.5.x-SNAPSHOT\\artifactory-plugin")
                .addSystemProp("classworlds.conf", "C:\\Work\\buildInfo\\build-info\\build-info-extractor-maven3\\src\\test\\resources\\it\\classworlds-freestyle.conf")

                .addTask("clean")
                .addTask("install")

//        config.pkgLabels.eachWithIndex { label, index ->
//            launcher.addProjProp("label${index+1}", label)
//        }
        launcher
    }

    def static launchMaven() {
        createGradleLauncher().launch()
    }
}
