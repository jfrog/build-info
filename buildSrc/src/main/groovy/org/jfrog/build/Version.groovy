package org.jfrog.build

import org.gradle.api.GradleException
import org.gradle.api.Project
import java.text.SimpleDateFormat

class Version {
    String versionNumber
    Date buildTime
    Boolean release = null

    def Version(Project project) {
        this(project, null)
    }

    def Version(Project project, List<String> subProjects) {
        this.versionNumber = project.getProperty("${project.name}-version")
        this.release = Boolean.valueOf(project.getProperty("${project.name}-release"))
        File timestampFile = new File(project.buildDir, 'timestamp.txt')
        if (timestampFile.isFile()) {
            boolean uptodate = true
            def modified = timestampFile.lastModified()
            if (subProjects != null) {
                // Check timestamp by list of subprojects
                subProjects.each { spName ->
                    project.project(spName).fileTree('src/main').visit {fte ->
                        if (fte.file.isFile() && fte.lastModified > modified) {
                            uptodate = false
                            fte.stopVisiting()
                        }
                    }
                }
            } else {
                project.fileTree('src/main').visit {fte ->
                    if (fte.file.isFile() && fte.lastModified > modified) {
                        uptodate = false
                        fte.stopVisiting()
                    }
                }
            }
            if (!uptodate) {
                timestampFile.setLastModified(new Date().time)
            }
        } else {
            timestampFile.parentFile.mkdirs()
            timestampFile.createNewFile()
        }
        buildTime = new Date(timestampFile.lastModified())
        if (!release) {
            //def ts = new SimpleDateFormat('yyyyMMddHHmmss').format(buildTime)
            this.versionNumber += "-SNAPSHOT" // + ts
            /*
                    project.gradle.taskGraph.whenReady {graph ->
                        if (graph.hasTask(':releaseVersion')) {
                            release = true
                        } else {
                            this.versionNumber += "-" + getTimestamp()
                            release = false
                        }
                    }
            */
        }
    }

    String getStatus() {
        if (release) return 'release'
        return 'integration'
    }

    String toString() {
        versionNumber
    }
}
