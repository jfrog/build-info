/*
 *
 *  Copyright (C) 2011 JFrog Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * /
 */

package org.jfrog.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.text.DateFormat
import java.text.SimpleDateFormat

class Version {
    String versionNumber
    Date buildTime
    Boolean release = null
    int indexOfSnapshot

    def Version(Project project) {
        this(project, null)
    }

    def Version(Project project, List<String> subProjects) {
        this.versionNumber = project.getProperty("${project.name}-version")
        this.release = true
        if (versionNumber.endsWith('SNAPSHOT')) {
            this.release = false
            this.indexOfSnapshot = this.versionNumber.indexOf('SNAPSHOT')
        }
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
            this.versionNumber = this.versionNumber.substring(0, indexOfSnapshot - 1)
            this.versionNumber += "-" + "SNAPSHOT"
        }
    }

    String toString() {
        versionNumber
    }

    String getTimestamp() {
        // Convert local file timestamp to UTC
        def format = new SimpleDateFormat('yyyyMMddHHmmss')
        format.setCalendar(Calendar.getInstance(TimeZone.getTimeZone('UTC')));
        return format.format(buildTime)
    }

    String getStatus() {
        return release ? "release" : "integration"
    }

    boolean isRelease() {
        if (release == null) {
            throw new GradleException("Can't determine whether this is a release build before the task graph is populated")
        }
        return release
    }
}

class WriteVersionProperties extends DefaultTask {
    @Input
    String getVersion() { return project.version.toString() }

    @Input
    Date getBuildTime() { return project.version.buildTime }

    @OutputFile
    File propertiesFile

    @TaskAction
    def void generate() {
        logger.info('Write version properties to: {}', propertiesFile)
        Properties versionProperties = new Properties()
        versionProperties.putAll([
                'version': version,
                'buildTime': DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL).format(buildTime)
        ])
        propertiesFile.withOutputStream {
            versionProperties.store(it, '')
        }

    }
}
