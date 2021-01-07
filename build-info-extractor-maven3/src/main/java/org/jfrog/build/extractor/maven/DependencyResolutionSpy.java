package org.jfrog.build.extractor.maven;

import org.apache.commons.compress.utils.Sets;
import org.apache.maven.eventspy.AbstractEventSpy;
import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.project.DependencyResolutionResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.DependencyNode;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    Map<String, Set<String>> createRequirementsMap(DependencyNode dependencyNode) {
        Map<String, Set<String>> requirementsMap = new HashMap<>();

        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getModuleIdString(child);
            // Populate the direct children with "root" parent
            addToRequirementSet(requirementsMap, childGav, getModuleIdString(dependencyNode));
            createRequirementsMap(child, requirementsMap);
        }

        return requirementsMap;
    }

    /**
     * Recursively create a requirements map for transitive dependencies.
     *
     * @param dependencyNode  - The dependency node
     * @param requirementsMap - Output - The map to populate
     */
    private void createRequirementsMap(DependencyNode dependencyNode, Map<String, Set<String>> requirementsMap) {
        List<DependencyNode> children = dependencyNode.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        String gav = getModuleIdString(dependencyNode);
        for (DependencyNode child : dependencyNode.getChildren()) {
            String childGav = getModuleIdString(child);
            addToRequirementSet(requirementsMap, childGav, gav);
            createRequirementsMap(child, requirementsMap);
        }
    }

    /**
     * Add a requirement to the dependency.
     *
     * @param requirementsMap - The requirement map to modify
     * @param childGav        - The child dependency GAV to modify
     * @param gav             - The parent dependency GAV to add to the set
     */
    private void addToRequirementSet(Map<String, Set<String>> requirementsMap, String childGav, String gav) {
        Set<String> requiredBy = requirementsMap.get(childGav);
        if (requiredBy != null) {
            requiredBy.add(gav);
        } else {
            requirementsMap.put(childGav, Sets.newHashSet(gav));
        }
    }

    private String getModuleIdString(DependencyNode dependencyNode) {
        Artifact artifact = dependencyNode.getArtifact();
        return BuildInfoExtractorUtils.getModuleIdString(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

}
