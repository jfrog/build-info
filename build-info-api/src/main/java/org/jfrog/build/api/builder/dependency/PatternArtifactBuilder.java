/*
 * Copyright (C) 2012 JFrog Ltd.
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

package org.jfrog.build.api.builder.dependency;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.dependency.PatternArtifact;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class PatternArtifactBuilder {

    private String artifactoryUrl;
    private String uri;
    private long size;
    private String lastModified;
    private String sha1;

    public PatternArtifact build() {
        if (artifactoryUrl == null) {
            throw new IllegalArgumentException("PatternArtifact must have an Artifactory URL.");
        }
        if (uri == null) {
            throw new IllegalArgumentException("PatternArtifact must have a URI.");
        }
        if (size == 0) {
            throw new IllegalArgumentException("PatternArtifact must have a size.");
        }
        if (lastModified == null) {
            throw new IllegalArgumentException("PatternArtifact must have a last Modified date.");
        }
        if (sha1 == null) {
            throw new IllegalArgumentException("PatternArtifact must have a sha1 checksum.");
        }
        return new PatternArtifact(artifactoryUrl, uri, size, lastModified, sha1);
    }

    public PatternArtifactBuilder artifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
        return this;
    }

    public PatternArtifactBuilder uri(String uri) {
        this.uri = uri;
        return this;
    }

    public PatternArtifactBuilder size(long size) {
        this.size = size;
        return this;
    }

    public PatternArtifactBuilder lastModified(String lastModified) {
        this.lastModified = lastModified;
        return this;
    }

    public PatternArtifactBuilder lastModifiedDate(Date lastModified) {
        if (lastModified == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.lastModified = new SimpleDateFormat(Build.STARTED_FORMAT).format(lastModified);
        return this;
    }

    public PatternArtifactBuilder sha1(String sha1) {
        this.sha1 = sha1;
        return this;
    }


}
