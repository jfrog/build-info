package org.jfrog.build.extractor.maven;

import org.apache.commons.lang.ArrayUtils;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.project.DependencyResolutionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.util.*;

/**
 * Listen to DependencyResolutionResult events and populate parents map for each dependency of the current module.
 *
 * @author yahavi
 **/
@Component(role = EventSpy.class)
public class DependencyResolutionSpy extends AbstractEventSpy {

    @SuppressWarnings("unused")
    @Requirement
    private BuildInfoRecorder buildInfoRecorder;

    @Override
    public void onEvent(Object event) {
        if (event instanceof DependencyResolutionResult) {
            DependencyResolutionResult result = (DependencyResolutionResult) event;
            buildInfoRecorder.setRequirementsMap(createRequirementsMap(result.getDependencyGraph()));
        }
    }

    /**
     * Create a map of dependency to set of parents.
     *
     * @param dependencyNode - The root dependency node
     * @return map of dependency to set of parents.
     */
    Map<String, String[][]> createRequirementsMap(DependencyNode dependencyNode) {
        Map<String, String[][]> requirementsMap = new HashMap<>();

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getModuleIdString(child);
            // Populate the direct children with "root" parent
            List<String> parents = Collections.singletonList(getModuleIdString(dependencyNode));
            addToRequirementSet(requirementsMap, childGav, parents);
            createRequirementsMap(child, requirementsMap, parents);
        }
        return requirementsMap;
    }

    /**
     * Recursively create a requirements map for transitive dependencies.
     *
     * @param dependencyNode  - The dependency node
     * @param requirementsMap - Output - The map to populate
     */
    private void createRequirementsMap(DependencyNode dependencyNode, Map<String, String[][]> requirementsMap, List<String> parents) {
        List<DependencyNode> children = dependencyNode.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        String gav = getModuleIdString(dependencyNode);
        List<String> currentParents = new ArrayList<>(parents);
        currentParents.add(0, gav);
        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getModuleIdString(child);
            addToRequirementSet(requirementsMap, childGav, currentParents);
            createRequirementsMap(child, requirementsMap, currentParents);
        }
    }

    /**
     * Add a requirement to the dependency.
     *
     * @param requirementsMap - The requirement map to modify
     * @param childGav        - The child dependency GAV to modify
     * @param parents         - The parent dependency GAV to add to the set
     */
    private void addToRequirementSet(Map<String, String[][]> requirementsMap, String childGav, List<String> parents) {
        String[][] requiredBy = requirementsMap.get(childGav);
        requiredBy = (String[][]) ArrayUtils.add(requiredBy, parents.toArray(new String[0]));
        requirementsMap.put(childGav, requiredBy);
    }

    private String getModuleIdString(DependencyNode dependencyNode) {
        Artifact artifact = dependencyNode.getArtifact();
        return BuildInfoExtractorUtils.getModuleIdString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

}
