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

package org.jfrog.build.api.dependency;

/**
 * Represents artifact matched by requested pattern. Part of {@link PatternResult}.
 *
 * @author jbaruch
 * @see BuildOutputs
 * @since 16/02/12
 */
public class PatternArtifact {

    private String uri;
    private long size;
    private String lastModified;
    private String sha1;

    public PatternArtifact() {
    }

    public PatternArtifact(String uri, long size, String lastModified, String sha1) {
        this.uri = uri;
        this.size = size;
        this.lastModified = lastModified;
        this.sha1 = sha1;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }
}
