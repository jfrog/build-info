package org.jfrog.build.extractor.go.dependencyTree;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.go.GoDriver;
import org.jfrog.build.extractor.scan.DependenciesTree;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Bar Belity on 13/02/2020.
 */
public class GoDependencyTree {

    public static DependenciesTree createDependenciesTree(GoDriver goDriver) throws IOException {
        // Run go mod graph.
        CommandResults goGraphResult = goDriver.goModGraph(false);
        String[] dependenciesGraph = goGraphResult.getRes().split("\\r?\\n");

        // Create root node.
        String rootPackageName = goDriver.getModuleName();
        DependenciesTree rootNode = new DependenciesTree(new GoPackageInfo(goDriver.getModuleName(), null));

        // Build dependencies tree.
        Map<String, List<String>> allDependencies = new HashMap<>();
        populateAllDependenciesMap(dependenciesGraph, allDependencies);
        populateDependenciesTree(rootNode, rootPackageName, allDependencies);

        return rootNode;
    }

    static void populateAllDependenciesMap(String[] dependenciesGraph, Map<String, List<String>> allDependencies) {
        for (String entry : dependenciesGraph) {
            if (StringUtils.isAllBlank(entry)) {
                continue;
            }
            String[] parsedEntry = entry.split("\\s");
            List<String> pkgDeps = allDependencies.get(parsedEntry[0]);
            if (pkgDeps == null) {
                pkgDeps = new ArrayList<>();
                allDependencies.put(parsedEntry[0], pkgDeps);
            }
            pkgDeps.add(parsedEntry[1]);
        }
    }

    static void populateDependenciesTree(DependenciesTree currNode, String currNameVersionString, Map<String, List<String>> allDependencies) {
        List<String> currDependencies = allDependencies.get(currNameVersionString);
        if (currDependencies == null) {
            return;
        }
        for (String dependency : currDependencies) {
            String[] dependencyNameVersion = dependency.split("@v");
            DependenciesTree dependenciesTree = new DependenciesTree(new GoPackageInfo(dependencyNameVersion[0], dependencyNameVersion[1]));
            populateDependenciesTree(dependenciesTree, dependency, allDependencies);
            currNode.add(dependenciesTree);
        }
    }
}
