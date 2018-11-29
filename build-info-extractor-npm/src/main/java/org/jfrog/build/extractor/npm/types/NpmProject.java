package org.jfrog.build.extractor.npm.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of scopes and the root node of 'npm ls' command for each scope.
 * Used by the npm extractor.
 *
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmProject {
    private List<Pair<NpmScope, JsonNode>> dependencies;

    public NpmProject() {
        dependencies = new ArrayList<>();
    }

    public List<Pair<NpmScope, JsonNode>> getDependencies() {
        return dependencies;
    }

    @SuppressWarnings("unused")
    public void addDependencies(Pair<NpmScope, JsonNode> dependencies) {
        this.dependencies.add(dependencies);
    }
}
