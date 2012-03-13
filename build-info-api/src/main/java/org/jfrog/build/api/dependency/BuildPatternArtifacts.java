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

import java.io.Serializable;
import java.util.List;

/**
 * Represents built artifacts, filtered by patterns in request.
 *
 * @author jbaruch
 * @see BuildPatternArtifactsRequest
 * @since 16/02/12
 */
public class BuildPatternArtifacts implements Serializable {

    private String buildName;
    private String buildNumber;
    private String started;
    private String url;

    private List<PatternResult> patternResults;

    public BuildPatternArtifacts() {
        patternResults = Lists.newArrayList();
    }

    public BuildPatternArtifacts(String buildName, String buildNumber, String started, String url) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
        this.started = started;
        this.url = url;
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

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<PatternResult> getPatternResults() {
        return patternResults;
    }

    public void setPatternResults(List<PatternResult> patternResults) {
        this.patternResults = patternResults;
    }

    public void addPatternResult(PatternResult patternResult) {
        if (patternResults == null) {
            patternResults = Lists.newArrayList();
        }
        patternResults.add(patternResult);
    }
}
