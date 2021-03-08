package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.scan.DependenciesTree;
import org.jfrog.build.extractor.scan.Scope;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"WeakerAccess"})
public class NpmDependencyTree {

    /**
     * Create a npm dependencies tree from the results of 'npm ls' command.
     *
     * @param npmList - Results of 'npm ls' command.
     * @return Tree of npm PackageInfos.
     * @see NpmPackageInfo
     */
    public static DependenciesTree createDependenciesTree(JsonNode npmList, NpmScope scope) {
        DependenciesTree rootNode = new DependenciesTree();
        populateDependenciesTree(rootNode, npmList.get("dependencies"), new String[]{npmList.get("name").asText() + ":" + npmList.get("version").asText()}, scope);
        for (DependenciesTree child : rootNode.getChildren()) {
            NpmPackageInfo packageInfo = (NpmPackageInfo) child.getUserObject();
            child.setScopes(getScopes(packageInfo.getName(), packageInfo.getScope()));
        }
        return rootNode;
    }

    /**
     * Parses npm dependencies recursively and adds the collected dependencies to scanTreeNode.
     *
     * @param scanTreeNode - Output - The DependenciesTree to populate.
     * @param dependencies - The dependencies json object generated by npm ls.
     * @param pathToRoot   - A path-to-root dependency list. The structure of each dependency in the list is 'dependency-name:dependency-version'.
     */
    private static void populateDependenciesTree(DependenciesTree scanTreeNode, JsonNode dependencies, String[] pathToRoot, NpmScope scope) {
        if (dependencies == null || pathToRoot == null) {
            return;
        }

        dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
            String name = stringJsonNodeEntry.getKey();
            JsonNode versionNode = stringJsonNodeEntry.getValue().get("version");
            if (versionNode != null) {
                addSubtree(stringJsonNodeEntry, scanTreeNode, name, versionNode.asText(), pathToRoot, scope); // Mutual recursive call
            }
        });
    }

    private static void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, DependenciesTree node, String name, String version, String[] pathToRoot, NpmScope scope) {
        JsonNode jsonNode = stringJsonNodeEntry.getValue();
        String devScope = scope.toString();
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, version, devScope, pathToRoot);
        JsonNode childDependencies = jsonNode.get("dependencies");
        DependenciesTree childTreeNode = new DependenciesTree(npmPackageInfo);
        populateDependenciesTree(childTreeNode, childDependencies, ArrayUtils.insert(0, pathToRoot, npmPackageInfo.toString()), scope); // Mutual recursive call
        node.add(childTreeNode);
    }

    /**
     * Return a set of the relevant scopes. The set contains 'development' or 'production'. If the dependency has a
     * custom scope, add it too.
     *
     * @param name     - The name of the dependency
     * @param devScope - 'development' or 'production'
     * @return set of the relevant scopes
     */
    private static Set<Scope> getScopes(String name, String devScope) {
        Set<Scope> scopes = new HashSet<>();
        scopes.add(new Scope(devScope));
        String customScope = StringUtils.substringBetween(name, "@", "/");
        if (customScope != null) {
            scopes.add(new Scope(customScope));
        }
        return scopes;
    }
}
