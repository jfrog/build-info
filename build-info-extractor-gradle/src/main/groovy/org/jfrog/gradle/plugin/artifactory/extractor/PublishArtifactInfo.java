/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.artifacts.PublishArtifact;

import java.io.File;

/**
 * Minimal impl of a publish artifact model
 *
 * @author Yoav Landman
 */
public class PublishArtifactInfo implements Comparable<PublishArtifactInfo> {

    private final String name;
    private final String extension;
    private final String type;
    private final String classifier;
    private final File file;

    public PublishArtifactInfo(PublishArtifact artifact) {
        this.name = artifact.getName();
        this.extension = artifact.getExtension();
        this.type = artifact.getType();
        this.classifier = artifact.getClassifier();
        this.file = artifact.getFile();
    }

    public PublishArtifactInfo(String name, String extension, String type, String classifier, File file) {
        this.name = name;
        this.extension = extension;
        this.type = type;
        this.classifier = classifier;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extension;
    }

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public File getFile() {
        return file;
    }
    
    public int compareTo(PublishArtifactInfo other) {
        return file.compareTo(other.getFile());
    }
}
