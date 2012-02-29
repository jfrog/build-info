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

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Represents request for built artifacts, filtered by patterns.
 *
 * @author jbaruch
 * @see BuildPatternArtifacts
 * @since 16/02/12
 */
public class BuildPatternArtifactsRequest {

    private String buildName;
    private String buildNumber;
    private boolean transitive;
    private List<Pattern> patterns;

    public BuildPatternArtifactsRequest() {
        patterns = Lists.newArrayList();
    }

    public BuildPatternArtifactsRequest(String buildName, String buildNumber) {
        this.buildNumber = buildNumber;
        this.buildName = buildName;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }

    public boolean isTransitive() {
        return transitive;
    }

    public void setTransitive(boolean transitive) {
        this.transitive = transitive;
    }

    public List<Pattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<Pattern> patterns) {
        this.patterns = patterns;
    }

    public void addPattern(Pattern pattern) {
        if (patterns == null) {
            patterns = Lists.newArrayList();
        }
        patterns.add(pattern);
    }
}
