package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.extractor.npm.types.NpmScope;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Map;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings({"WeakerAccess"})
public class NpmDependencyTree {

    public static DefaultMutableTreeNode getDependenciesTree(NpmScope scope, JsonNode npmList) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        populateDependenciesTree(rootNode, scope, npmList.get("dependencies"));
        return rootNode;
    }

    private static void populateDependenciesTree(DefaultMutableTreeNode scanTreeNode, NpmScope scope, JsonNode dependencies) {
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

    private static void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, DefaultMutableTreeNode node, String name, String version, NpmScope scope) {
        PackageInfo packageInfo = new PackageInfo(name, version, scope.toString());
        JsonNode childDependencies = stringJsonNodeEntry.getValue().get("dependencies");
        DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(packageInfo);
        populateDependenciesTree(childTreeNode, scope, childDependencies); // Mutual recursive call
        node.add(childTreeNode);
    }
}
