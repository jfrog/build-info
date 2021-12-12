package org.jfrog.build.api.dependency;

import java.io.Serializable;
import java.util.ArrayList;
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
        patternArtifacts = new ArrayList<>();
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
            patternArtifacts = new ArrayList<>();
        }
        patternArtifacts.add(patternArtifact);
    }
}
