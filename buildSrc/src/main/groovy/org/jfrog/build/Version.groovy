package org.jfrog.build

import java.text.SimpleDateFormat
import org.gradle.api.GradleException
import org.gradle.api.Project

class Version {
  String versionNumber
  Date buildTime
  Boolean release = null

  def Version(Project project) {
    this.versionNumber = project.nextVersion
    this.release = Boolean.parseBoolean(project.isRelease)
    File timestampFile = new File(project.buildDir, 'timestamp.txt')
    if (timestampFile.isFile()) {
      boolean uptodate = true
      def modified = timestampFile.lastModified()
      project.fileTree('src/main').visit {fte ->
        if (fte.file.isFile() && fte.lastModified > modified) {
          uptodate = false
          fte.stopVisiting()
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
    if (!release)
      this.versionNumber += "-" + getTimestamp()
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

  String toString() {
    versionNumber
  }

  String getTimestamp() {
    new SimpleDateFormat('yyyyMMddHHmmssZ').format(buildTime)
  }

  boolean isRelease() {
    if (release == null) {
      throw new GradleException("Can't determine whether this is a release build before the task graph is populated")
    }
    return release
  }
}
