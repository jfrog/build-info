package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.scan.DependencyTree;
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
     * Create a npm dependency tree from the results of 'npm ls' command.
     *
     * @param npmList - Results of 'npm ls' command.
     * @return Tree of npm PackageInfos.
     * @see NpmPackageInfo
     */
    public static DependencyTree createDependencyTree(JsonNode npmList) {
        DependencyTree rootNode = new DependencyTree();
        populateDependencyTree(rootNode, npmList.get("dependencies"));
        for (DependencyTree child : rootNode.getChildren()) {
            NpmPackageInfo packageInfo = (NpmPackageInfo) child.getUserObject();
            child.setScopes(getScopes(packageInfo.getName(), packageInfo.getScope()));
        }
        return rootNode;
    }

    private static void populateDependencyTree(DependencyTree scanTreeNode, JsonNode dependencies) {
        if (dependencies == null) {
            return;
        }

        dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
            String name = stringJsonNodeEntry.getKey();
            JsonNode versionNode = stringJsonNodeEntry.getValue().get("version");
            if (versionNode != null) {
                addSubtree(stringJsonNodeEntry, scanTreeNode, name, versionNode.asText()); // Mutual recursive call
            }
        });
    }

    private static void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, DependencyTree node, String name, String version) {
        JsonNode jsonNode = stringJsonNodeEntry.getValue();
        String devScope = (isDev(jsonNode) ? NpmScope.DEVELOPMENT : NpmScope.PRODUCTION).toString();
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, version, devScope);
        JsonNode childDependencies = jsonNode.get("dependencies");
        DependencyTree childTreeNode = new DependencyTree(npmPackageInfo);
        populateDependencyTree(childTreeNode, childDependencies); // Mutual recursive call
        node.add(childTreeNode);
    }

    /**
     * Return true if the input dependency is a dev dependency.
     *
     * @param jsonNode - The dependency node in the 'npm ls' results
     * @return true if the input dependency is a dev dependency
     */
    private static boolean isDev(JsonNode jsonNode) {
        JsonNode development = jsonNode.get("_development");
        return development != null && development.asBoolean(false);
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
