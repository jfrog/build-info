package org.jfrog.build.extractor.go.extractor;

import com.google.common.collect.Sets;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.scan.DependencyTree;
import org.testng.annotations.Test;

import java.util.*;

import static org.jfrog.build.extractor.go.extractor.GoDependencyTree.populateDependenciesMap;
import static org.jfrog.build.extractor.go.extractor.GoDependencyTree.populateDependencyTree;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class GoDependencyTreeTest {

    @Test
    public void testPopulateDependenciesMap() {
        // Output of 'go mod graph'.
        String[] dependenciesGraph = new String[]{
                "my/pkg/name1 github.com/jfrog/directDep1@v0.1",
                "my/pkg/name1 github.com/jfrog/directDep2@v0.2",
                "my/pkg/name1 github.com/jfrog/directDep3@v0.3",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep1-1@v1.1",
                // github.com/jfrog/indirectDep2-1@v1.2 does not appear in the used modules set:
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep2-1@v1.2",
                "github.com/jfrog/directDep1@v0.1 github.com/jfrog/indirectDep2-1@v1.3",
                "github.com/jfrog/directDep2@v0.2 github.com/jfrog/indirectDep1-2@v2.1",
                "github.com/jfrog/indirectDep1-1@v1.1 github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"
        };
        Set<String> usedModules = Sets.newHashSet("github.com/jfrog/directDep1@v0.1",
                "github.com/jfrog/directDep2@v0.2",
                "github.com/jfrog/directDep3@v0.3",
                "github.com/jfrog/indirectDep1-1@v1.1",
                "github.com/jfrog/indirectDep2-1@v1.3",
                "github.com/jfrog/indirectDep1-2@v2.1",
                "github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1");
        // Run method.
        Map<String, List<String>> expectedAllDependencies = getAllDependenciesForTest();
        Map<String, List<String>> actualAllDependencies = new HashMap<>();
        populateDependenciesMap(dependenciesGraph, usedModules, actualAllDependencies);
        // Validate result.
        assertEquals(expectedAllDependencies, actualAllDependencies);
    }

    @Test
    public void testPopulateDependencyTree() {
        Map<String, Integer> expected = new HashMap<String, Integer>() {{
            put("github.com/jfrog/directDep1:0.1", 2);
            put("github.com/jfrog/directDep2:0.2", 1);
            put("github.com/jfrog/directDep3:0.3", 0);
        }};
        Map<String, List<String>> allDependencies = getAllDependenciesForTest();
        DependencyTree rootNode = new DependencyTree();
        populateDependencyTree(rootNode, "my/pkg/name1", allDependencies, new NullLog());

        Vector<DependencyTree> children = rootNode.getChildren();
        assertEquals(children.size(), expected.size());
        for (DependencyTree current : children) {
            assertTrue(expected.containsKey(current.toString()));
            assertEquals(current.getChildren().size(), expected.get(current.toString()).intValue());
        }
    }

    private Map<String, List<String>> getAllDependenciesForTest() {
        return new HashMap<String, List<String>>() {{
            put("my/pkg/name1", Arrays.asList(
                    "github.com/jfrog/directDep1@v0.1",
                    "github.com/jfrog/directDep2@v0.2",
                    "github.com/jfrog/directDep3@v0.3"));
            put("github.com/jfrog/directDep1@v0.1", List.of(
                    "github.com/jfrog/indirectDep1-1@v1.1",
                    "github.com/jfrog/indirectDep2-1@v1.3"));
            put("github.com/jfrog/directDep2@v0.2", Collections.singletonList(
                    "github.com/jfrog/indirectDep1-2@v2.1"));
            put("github.com/jfrog/indirectDep1-1@v1.1", Collections.singletonList(
                    "github.com/jfrog/indirectIndirectDep1-1-1@v1.1.1"));
        }};
    }
}
