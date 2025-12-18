package org.jfrog.build.extractor.maven;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.project.DependencyResolutionResult;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listen to DependencyResolutionResult events and populate parents map for each dependency of the current module.
 * Each parent is an array of strings, containing the path from parent GAV to module GAV.
 *
 * @author yahavi
 **/
@Singleton
@Named
public class DependencyResolutionSpy extends AbstractEventSpy {

    private final BuildInfoRecorder buildInfoRecorder;

    @Inject
    public DependencyResolutionSpy(BuildInfoRecorder buildInfoRecorder) {
        this.buildInfoRecorder = buildInfoRecorder;
    }

    @Override
    public void onEvent(Object event) {
        if (event instanceof DependencyResolutionResult) {
            DependencyResolutionResult result = (DependencyResolutionResult) event;
            buildInfoRecorder.setDependencyParentsMaps(createDependencyParentsMap(result.getDependencyGraph()));
        }
    }

    /**
     * Create a map of dependency to parents.
     * Key - dependency ID - group:artifact:version.
     * Value - parents path-to-module. For example:
     * [["parentIdA", "a1", "a2",... "moduleId"]
     * ["parentIdB", "b1", "b2",... "moduleId"]]
     *
     * @param dependencyNode - The root dependency node
     * @return map of dependency to parents.
     * @see org.jfrog.build.api.Dependency#setRequestedBy(String[][])
     */
    Map<String, String[][]> createDependencyParentsMap(DependencyNode dependencyNode) {
        Map<String, String[][]> dependencyParentsMap = new HashMap<>();

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getGavString(child);
            // Populate the direct children with the module's GAV
            List<String> parents = Collections.singletonList(getGavString(dependencyNode));
            addParent(dependencyParentsMap, childGav, parents);

            // Create dependency parent map for children
            createDependencyParentsMap(dependencyParentsMap, child, parents);
        }
        return dependencyParentsMap;
    }

    /**
     * Recursively create a requirements map for transitive dependencies.
     *
     * @param dependencyParentsMap - Output - The map to populate
     * @param dependencyNode       - The current dependency node
     * @param parent               - The parent path-to-module list
     */
    private void createDependencyParentsMap(Map<String, String[][]> dependencyParentsMap, DependencyNode dependencyNode, List<String> parent) {
        List<DependencyNode> children = dependencyNode.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }

        // Create the parent path-to-module for the children
        List<String> childParents = new ArrayList<>(parent);
        childParents.add(0, getGavString(dependencyNode));

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getGavString(child);
            addParent(dependencyParentsMap, childGav, childParents);
            createDependencyParentsMap(dependencyParentsMap, child, childParents);
        }
    }

    /**
     * Add parent to the dependency.
     *
     * @param dependencyParentsMap - The dependency parents map
     * @param childGav             - The child dependency GAV
     * @param parent               - The parent path-to-module list to add to the map
     */
    private void addParent(Map<String, String[][]> dependencyParentsMap, String childGav, List<String> parent) {
        // Get current parents
        String[][] currentParents = dependencyParentsMap.getOrDefault(childGav, new String[][]{});

        // Add the input parent to the current parents
        currentParents = (String[][]) ArrayUtils.add(currentParents, parent.toArray(ArrayUtils.EMPTY_STRING_ARRAY));

        // Set the updated parents
        dependencyParentsMap.put(childGav, currentParents);
    }

    private String getGavString(DependencyNode dependencyNode) {
        Artifact artifact = dependencyNode.getArtifact();
        return BuildInfoExtractorUtils.getModuleIdString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

}
