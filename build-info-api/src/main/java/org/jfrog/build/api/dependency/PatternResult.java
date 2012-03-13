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
 * Represents group of results for pattern request in {@link BuildPatternArtifactsRequest}. Part of {@link BuildPatternArtifacts}.
 *
 * @author jbaruch
 * @see BuildPatternArtifacts
 * @since 16/02/12
 */
public class PatternResult implements Serializable {

    private List<PatternArtifact> patternArtifacts;

    public PatternResult() {
        patternArtifacts = Lists.newArrayList();
    }

    public PatternResult(List<PatternArtifact> patternArtifacts) {
        this.patternArtifacts = patternArtifacts;
    }

    public List<PatternArtifact> getPatternArtifacts() {
        return patternArtifacts;
    }

    public void setPatternArtifacts(List<PatternArtifact> patternArtifacts) {
        this.patternArtifacts = patternArtifacts;
    }

    public void addArtifact(PatternArtifact patternArtifact) {
        if (patternArtifacts == null) {
            patternArtifacts = Lists.newArrayList();
        }
        patternArtifacts.add(patternArtifact);
    }
}
