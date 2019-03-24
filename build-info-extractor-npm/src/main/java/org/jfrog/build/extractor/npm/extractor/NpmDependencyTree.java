package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.ObjectUtils;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.scan.DependenciesTree;

import java.util.Map;

/**
 * @author Yahav Itzhak
 */
@SuppressWarnings({"WeakerAccess"})
public class NpmDependencyTree {

    /**
     * Create a npm dependencies tree from the results of 'npm ls' command.
     *
     * @param scope   - 'production' or 'development'.
     * @param npmList - Results of 'npm ls' command.
     * @return Tree of npm PackageInfos.
     * @see NpmPackageInfo
     */
    public static DependenciesTree createDependenciesTree(NpmScope scope, JsonNode npmList) {
        DependenciesTree rootNode = new DependenciesTree();
        populateDependenciesTree(rootNode, scope, npmList.get("dependencies"));
        return rootNode;
    }

    private static void populateDependenciesTree(DependenciesTree scanTreeNode, NpmScope scope, JsonNode dependencies) {
        if (dependencies == null) {
            return;
        }

        dependencies.fields().forEachRemaining(stringJsonNodeEntry -> {
            String name = stringJsonNodeEntry.getKey();
            JsonNode versionNode = stringJsonNodeEntry.getValue().get("version");
            if (versionNode != null) {
                addSubtree(stringJsonNodeEntry, scanTreeNode, name, versionNode.asText(), scope); // Mutual recursive call
            }
        });
    }

    private static void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, DependenciesTree node, String name, String version, NpmScope scope) {
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, version, ObjectUtils.defaultIfNull(scope, "").toString());
        JsonNode childDependencies = stringJsonNodeEntry.getValue().get("dependencies");
        DependenciesTree childTreeNode = new DependenciesTree(npmPackageInfo);
        populateDependenciesTree(childTreeNode, scope, childDependencies); // Mutual recursive call
        node.add(childTreeNode);
    }
}
