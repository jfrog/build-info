package org.jfrog.gradle.plugin.artifactory.extractor.listener

import org.apache.commons.lang3.ArrayUtils
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString

/**
 * Represents a DependencyResolutionListener, used to populate a dependency hierarchy map for each dependency in each module,
 * which is used in the 'requestedBy' field of every dependency in the build info.
 * Does so by listening to the 'afterResolve' event of every module.
 */
@SuppressWarnings("unused")
class ArtifactoryDependencyResolutionListener implements DependencyResolutionListener {
    final Map<String, Map<String, String[][]>> modulesHierarchyMap = new HashMap()

    @Override
    void beforeResolve(ResolvableDependencies dependencies) {
    }

    @Override
    void afterResolve(ResolvableDependencies dependencies) {
        if (!dependencies.getResolutionResult().getAllDependencies().isEmpty()) {
            updateModulesHierarchyMap(dependencies)
        }
    }

    /**
     * Handles the modules' hierarchy map update.
     * @param dependencies - Module's resolved dependencies.
     */
    @SuppressWarnings("unused")
    void updateModulesHierarchyMap(ResolvableDependencies dependencies) {
        String compId = getGav(dependencies.getResolutionResult().getRoot().getModuleVersion())
        // If the module was already visited, update it.
        Map<String, String[][]> hierarchyMap = (Map<String, String[][]>) modulesHierarchyMap.get(compId)
        if (hierarchyMap == null) {
            hierarchyMap = new HashMap<>()
            modulesHierarchyMap.put(compId, hierarchyMap)
        }
        updateDependencyMap(hierarchyMap, dependencies.getResolutionResult().getAllDependencies())
    }

    /**
     * Iterates over each resolved dependency and updates the map with it's parents.
     * @param hierarchyMap - Dependency map of the module.
     * @param dependencies - Set of the resolved dependencies.
     */
    private void updateDependencyMap(Map<String, String[][]> hierarchyMap, Set<DependencyResult> dependencies) {
        for (DependencyResult dependency : dependencies) {
            // Update the map for every resolved dependency.
            if (dependency instanceof ResolvedDependencyResult) {
                String compId = getGav(dependency.getSelected().getModuleVersion())
                String[][] curDependents = hierarchyMap[compId]
                // If already collected for this compId, skip.
                if (curDependents == null) {
                    List<String> newDependentsList = new ArrayList<>()
                    populateDependentsList(dependency, newDependentsList)
                    // Add the new dependent's path to root to the 2d array.
                    curDependents = ArrayUtils.add(curDependents, newDependentsList as String[])
                    hierarchyMap[compId] = curDependents
                }
            }
        }
    }

    /**
     * Recursively populate a pathToRoot list of transitive dependencies. Root is expected to be last in list.
     * @param dependency - To populate the dependents list for.
     * @param dependents - Dependents list to populate.
     */
    private void populateDependentsList(ResolvedDependencyResult dependency, List<String> dependents) {
        ResolvedComponentResult from = dependency.getFrom()
        if (from.getDependents().isEmpty()) {
            // If the dependency was requested by root, append the root's GAV.
            if (from.getSelectionReason().isExpected()) {
                dependents << getGav(from.getModuleVersion())
                return
            }
            // Unexpected result.
            throw new RuntimeException("Failed populating dependency parents map: dependency has no dependents and is not root.")
        }
        // We assume the first parent in the list, is the item that triggered this dependency resolution.
        ResolvedDependencyResult parent = from.getDependents().iterator().next()
        String parentGav = getGav(parent.getSelected().getModuleVersion())
        // Check for circular dependencies loop. We do this check to avoid an infinite loop dependencies. For example: A --> B --> C --> A...
        if (dependents.contains(parentGav)) {
            return
        }
        // Append the current parent's GAV to list.
        dependents << parentGav
        // Continue populating dependents list.
        populateDependentsList(parent, dependents)
    }

    private static String getGav(ModuleVersionIdentifier module) {
        return getModuleIdString(module.getGroup(), module.getName(), module.getVersion())
    }
}
