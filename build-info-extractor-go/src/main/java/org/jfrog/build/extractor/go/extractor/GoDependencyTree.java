package org.jfrog.build.extractor.go.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.scan.DependencyTree;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yahavi
 **/
public class GoDependencyTree {

    /**
     * Create Go dependency tree of actually used dependencies.
     *
     * @param goDriver     - Go driver
     * @param logger       - The logger
     * @param verbose      - verbose logging
     * @param dontBuildVcs - Skip VCS stamping - can be used only on Go later than 1.18
     * @return Go dependency tree
     * @throws IOException in case of any I/O error.
     */
    public static DependencyTree createDependencyTree(GoDriver goDriver, Log logger, boolean verbose, boolean dontBuildVcs) throws IOException {
        // Run go mod graph.
        CommandResults goGraphResult = goDriver.modGraph(verbose);
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");

        // Run go list -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all
        CommandResults usedModulesResults;
        try {
            usedModulesResults = goDriver.getUsedModules(false, false, dontBuildVcs);
        } catch (IOException e) {
            // Errors occurred during running "go list". Run again and this time ignore errors.
            usedModulesResults = goDriver.getUsedModules(false, true, dontBuildVcs);
            logger.warn("Errors occurred during building the Go dependency tree. The dependency tree may be incomplete:" +
                    System.lineSeparator() + ExceptionUtils.getRootCauseMessage(e));
        }
        Set<String> usedDependencies = Arrays.stream(usedModulesResults.getRes().split("\\r?\\n"))
                .map(String::trim)
                .map(usedModule -> usedModule.replace(" ", "@"))
                .collect(Collectors.toSet());

        // Create root node.
        String rootPackageName = goDriver.getModuleName();
        DependencyTree rootNode = new DependencyTree(rootPackageName);
        rootNode.setMetadata(true);

        // Build dependency tree.
        Map<String, List<String>> dependenciesMap = new HashMap<>();
        populateDependenciesMap(dependenciesGraph, usedDependencies, dependenciesMap);
        populateDependencyTree(rootNode, rootPackageName, dependenciesMap, logger);

        return rootNode;
    }

    /**
     * Populate the parent to children map with dependencies which are actually in use in the project.
     *
     * @param dependenciesGraph - Results of "go mod graph"
     * @param usedDependencies  - Results of "go list -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all"
     * @param dependenciesMap   - Dependencies parent to children map results
     */
    static void populateDependenciesMap(String[] dependenciesGraph, Set<String> usedDependencies, Map<String, List<String>> dependenciesMap) {
        for (String entry : dependenciesGraph) {
            if (StringUtils.isAllBlank(entry)) {
                continue;
            }
            String[] parsedEntry = entry.split("\\s");
            if (!usedDependencies.contains(parsedEntry[1])) {
                // Module is not in use
                continue;
            }
            List<String> pkgDeps = dependenciesMap.computeIfAbsent(parsedEntry[0], k -> new ArrayList<>());
            pkgDeps.add(parsedEntry[1]);
        }
    }

    /**
     * Recursively populate the dependency tree.
     *
     * @param currNode              - The current node
     * @param currNameVersionString - Current dependency in form of <name>@v<version>
     * @param allDependencies       - Dependency to children map
     * @param logger                - The logger
     */
    static void populateDependencyTree(DependencyTree currNode, String currNameVersionString,
                                       Map<String, List<String>> allDependencies, Log logger) {
        if (currNode.hasLoop(logger)) {
            return;
        }
        List<String> currDependencies = allDependencies.get(currNameVersionString);
        if (currDependencies == null) {
            return;
        }
        for (String dependency : currDependencies) {
            String[] dependencyNameVersion = dependency.split("@v");
            DependencyTree DependencyTree = new DependencyTree(dependencyNameVersion[0] + ":" + dependencyNameVersion[1]);
            currNode.add(DependencyTree);
            populateDependencyTree(DependencyTree, dependency, allDependencies, logger);
        }
    }
}
