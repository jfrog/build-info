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
public class PublishArtifactInfo {

    private final String name;
    private final String extenstion;
    private final String type;
    private final String classfier;
    private final File file;

    public PublishArtifactInfo(PublishArtifact artifact) {
        this.name = artifact.getName();
        this.extenstion = artifact.getExtension();
        this.type = artifact.getType();
        this.classfier = artifact.getClassifier();
        this.file = artifact.getFile();
    }

    public PublishArtifactInfo(String name, String extenstion, String type, String classfier, File file) {
        this.name = name;
        this.extenstion = extenstion;
        this.type = type;
        this.classfier = classfier;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public String getExtension() {
        return extenstion;
    }

    public String getType() {
        return type;
    }

    public String getClassifier() {
        return classfier;
    }

    public File getFile() {
        return file;
    }
}
