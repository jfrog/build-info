package org.jfrog.gradle.plugin.artifactory.extractor.listener

import org.apache.commons.lang.ArrayUtils
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
                String[] newDependent = getPathToRoot(dependency)

                // Add the new dependent's path to root to the 2d array.
                String compId = getGav(dependency.getSelected().getModuleVersion())
                String[][] curDependants = hierarchyMap[compId]
                curDependants = ArrayUtils.add(curDependants, newDependent)
                hierarchyMap[compId] = curDependants
            }
        }
    }

    /**
     * Recursively populate a pathToRoot array of transitive dependencies.
     * @param dependency
     * @return
     */
    private String[] getPathToRoot(ResolvedDependencyResult dependency) {
        ResolvedComponentResult from = dependency.getFrom()
        if (from.getDependents().isEmpty()) {
            // If the dependency was requested by root, return an array with the root's GAV.
            if (from.getSelectionReason().isExpected()) {
                return [getGav(from.getModuleVersion())]
            }
            // Unexpected result.
            return new RuntimeException("Failed populating dependency parents map: dependency has no dependents and is not root.")
        }
        // Get parent's path to root, then add the parent's GAV.
        // We assume the first parent in the list, is the item that that triggered this dependency resolution.
        ResolvedDependencyResult parent = from.getDependents().iterator().next()
        List<String> dependants = getPathToRoot(parent)
        // Add the current parent to the beginning of the list.
        dependants.add(0, getGav(parent.getSelected().getModuleVersion()))
        return dependants
    }

    private static String getGav(ModuleVersionIdentifier module) {
        return getModuleIdString(module.getGroup(), module.getName(), module.getVersion())
    }
}
