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
 * Represents build outputs: build dependencies and built artifacts, filtered by patterns in request.
 * @see BuildOutputsRequest
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildOutputs {

    private String buildName;
    private String buildNumber;
    private List<BuildDependency> buildDependencies;
    private List<PatternResult> patternResults;

    public BuildOutputs() {
        buildDependencies = Lists.newArrayList();
        patternResults = Lists.newArrayList();
    }

    public BuildOutputs(String buildName, String buildNumber) {
        this.buildName = buildName;
        this.buildNumber = buildNumber;
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

    public List<BuildDependency> getBuildDependencies() {
        return buildDependencies;
    }

    public void setBuildDependencies(List<BuildDependency> buildDependencies) {
        this.buildDependencies = buildDependencies;
    }

    public void addBuildDependency(BuildDependency buildDependency){
        if(buildDependencies == null){
            buildDependencies = Lists.newArrayList();
        }
        buildDependencies.add(buildDependency);
    }

    public List<PatternResult> getPatternResults() {
        return patternResults;
    }

    public void setPatternResults(List<PatternResult> patternResults) {
        this.patternResults = patternResults;
    }

    public void addPatternResult(PatternResult patternResult){
        if(patternResults == null){
            patternResults = Lists.newArrayList();
        }
        patternResults.add(patternResult);
    }
}
