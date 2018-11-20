package org.jfrog.build.extractor.npm.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class NpmProject {
    private ArtifactoryDependenciesClient dependenciesClient;
    private List<Pair<NpmScope, JsonNode>> dependencies;
    private PrintStream logger;

    public NpmProject(ArtifactoryDependenciesClient dependenciesClient, PrintStream logger) {
        this.dependenciesClient = dependenciesClient;
        this.logger = logger;
        dependencies = new ArrayList<>();
    }

    public ArtifactoryDependenciesClient getDependenciesClient() {
        return dependenciesClient;
    }

    public PrintStream getLogger() {
        return logger;
    }

    public List<Pair<NpmScope, JsonNode>> getDependencies() {
        return dependencies;
    }

    @SuppressWarnings("unused")
    public void addDependencies(Pair<NpmScope, JsonNode> dependencies) {
        this.dependencies.add(dependencies);
    }
}
