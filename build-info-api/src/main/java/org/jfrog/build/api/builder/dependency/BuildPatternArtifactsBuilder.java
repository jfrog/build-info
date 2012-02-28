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

import com.google.common.collect.Lists;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.PatternResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsBuilder {
    private String buildName;
    private String buildNumber;
    private String started;
    private String url;
    private List<PatternResult> patternResults;

    public BuildPatternArtifactsBuilder() {
        patternResults = Lists.newArrayList();
    }

    public BuildPatternArtifacts build() {
        if (buildName == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a name.");
        }
        if (buildNumber == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a number.");
        }
        if (started == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a started.");
        }
        if (url == null) {
            throw new IllegalArgumentException("BuildPatternArtifacts must have a url.");
        }
        BuildPatternArtifacts buildPatternArtifacts = new BuildPatternArtifacts(buildName, buildNumber, started, url);
        buildPatternArtifacts.setPatternResults(patternResults);
        return buildPatternArtifacts;
    }

    public BuildPatternArtifactsBuilder buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildPatternArtifactsBuilder buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }

    public BuildPatternArtifactsBuilder patternResult(PatternResult patternResult) {
        patternResults.add(patternResult);
        return this;
    }

    public BuildPatternArtifactsBuilder started(String started) {
        this.started = started;
        return this;
    }

    public BuildPatternArtifactsBuilder startedDate(Date started) {
        if (started == null) {
            throw new IllegalArgumentException("Cannot format a null date.");
        }
        this.started = new SimpleDateFormat(Build.STARTED_FORMAT).format(started);
        return this;
    }


    public BuildPatternArtifactsBuilder url(String url) {
        this.url = url;
        return this;
    }
}
