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

import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.Pattern;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsRequestBuilder {

    private String buildName;
    private String buildNumber;
    private boolean transitive;
    private List<Pattern> patterns;

    public BuildPatternArtifactsRequestBuilder() {
        patterns = new ArrayList<>();
    }

    public BuildPatternArtifactsRequest build() {
        if (buildName == null) {
            throw new IllegalArgumentException("BuildPatternArtifactsRequest must have a build name.");
        }
        if (buildNumber == null) {
            throw new IllegalArgumentException("BuildPatternArtifactsRequest must have a build number.");
        }

        BuildPatternArtifactsRequest request = new BuildPatternArtifactsRequest(buildName, buildNumber);
        request.setTransitive(transitive);
        request.setPatterns(patterns);
        return request;
    }

    public BuildPatternArtifactsRequestBuilder buildName(String buildName) {
        this.buildName = buildName;
        return this;
    }

    public BuildPatternArtifactsRequestBuilder buildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
        return this;
    }
    //no support for transitivity

    public BuildPatternArtifactsRequestBuilder pattern(String pattern) {
        patterns.add(new Pattern(pattern));
        return this;
    }
}
