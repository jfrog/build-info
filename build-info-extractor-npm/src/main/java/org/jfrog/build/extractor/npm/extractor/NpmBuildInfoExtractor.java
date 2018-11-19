package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.api.ComponentDetail;
import org.jfrog.build.extractor.npm.utils.Scope;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.*;

public class NpmBuildInfoExtractor {
    private static ObjectMapper mapper = new ObjectMapper();
    private Map<String, ComponentDetail> dependencies = new HashMap<>();

    public List<ComponentDetail> getDependenciesList(String npmLsResults, Scope scope) throws IOException {
        DefaultMutableTreeNode rootNode = getDependenciesTree(npmLsResults, scope);
        Enumeration e = rootNode.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            ComponentDetail packageInfo = (ComponentDetail) node.getUserObject();
            if (packageInfo != null) {
                appendDependency(packageInfo.getComponentId(), packageInfo.getScopes());
            }
        }
        return new ArrayList<>(dependencies.values());
    }

    public static DefaultMutableTreeNode getDependenciesTree(String npmLsResults, Scope scope) throws IOException {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        JsonNode dependencies = mapper.readTree(npmLsResults).get("dependencies");
        populateDependenciesTree(rootNode, dependencies, scope);
        return rootNode;
    }

    private static void populateDependenciesTree(DefaultMutableTreeNode scanTreeNode, JsonNode dependencies, Scope scope) {
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

    private static void addSubtree(Map.Entry<String, JsonNode> stringJsonNodeEntry, DefaultMutableTreeNode node, String name, String version, Scope scope) {
        ComponentDetail packageInfo = new ComponentDetail(getId(name, version));
        JsonNode childDependencies = stringJsonNodeEntry.getValue().get("dependencies");
        DefaultMutableTreeNode childTreeNode = new DefaultMutableTreeNode(packageInfo);
        populateDependenciesTree(childTreeNode, childDependencies, scope); // Mutual recursive call
        node.add(childTreeNode);
    }

    private void appendDependency(String key, List<String> scopes) {
        ComponentDetail dependency = dependencies.get(key);
        if (dependency == null) {
            dependency = new ComponentDetail(key);
            dependencies.put(key, dependency);
        }
        dependency.addScopes(scopes);
    }

    private static String getId(String key, String version) {
        return key + ":" + version;
    }

}
