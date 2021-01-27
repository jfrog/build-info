package org.jfrog.build.extractor.maven;

import org.apache.commons.compress.utils.Sets;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.DefaultDependencyNode;
import org.eclipse.aether.graph.DependencyNode;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Arrays;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author yahavi
 **/
public class DependencyResolutionSpyTest {

    private static final String MODULE_GAV = "module-group:module-name:module-version";
    private static final String DEP_A_GAV = "a1:a2:a3";
    private static final String DEP_B_GAV = "b1:b2:b3";
    private static final String DEP_C_GAV = "c1:c2:c3";

    DependencyResolutionSpy dependencyResolutionSpy = new DependencyResolutionSpy();
    DependencyNode module;

    @BeforeMethod
    public void setUp() {
        module = createDependencyNode(MODULE_GAV);
    }

    /**
     * Test module without dependencies.
     */
    @Test
    public void emptyRequirementsMapTest() {
        Map<String, String[][]> requirementMaps = dependencyResolutionSpy.createRequirementsMap(module);
        assertEquals(requirementMaps.size(), 0);
    }

    /**
     * Test module with 3 direct dependencies.
     */
    @Test
    public void flatRequirementsMapTest() {
        DependencyNode a = createDependencyNode(DEP_A_GAV);
        DependencyNode b = createDependencyNode(DEP_B_GAV);
        DependencyNode c = createDependencyNode(DEP_C_GAV);
        module.setChildren(Lists.newArrayList(a, b, c));

        Map<String, String[][]> requirementMaps = dependencyResolutionSpy.createRequirementsMap(module);
        assertEquals(requirementMaps.size(), 3);
        assertEquals(requirementMaps.keySet(), Sets.newHashSet(DEP_A_GAV, DEP_B_GAV, DEP_C_GAV));
        requirementMaps.values().forEach(parents -> comparePathToModule(parents, new String[][]{{MODULE_GAV}}));
    }

    /**
     * Test module with 3 transitive dependencies: a->b->c
     */
    @Test
    public void deepRequirementsMapTest() {
        DependencyNode a = createDependencyNode(DEP_A_GAV);
        DependencyNode b = createDependencyNode(DEP_B_GAV);
        DependencyNode c = createDependencyNode(DEP_C_GAV);
        module.setChildren(Lists.newArrayList(a));
        a.setChildren(Lists.newArrayList(b));
        b.setChildren(Lists.newArrayList(c));

        Map<String, String[][]> requirementMaps = dependencyResolutionSpy.createRequirementsMap(module);
        assertEquals(requirementMaps.size(), 3);
        assertEquals(requirementMaps.keySet(), Sets.newHashSet(DEP_A_GAV, DEP_B_GAV, DEP_C_GAV));
        comparePathToModule(requirementMaps.get(DEP_A_GAV), new String[][]{{MODULE_GAV}});
        comparePathToModule(requirementMaps.get(DEP_B_GAV), new String[][]{{DEP_A_GAV, MODULE_GAV}});
        comparePathToModule(requirementMaps.get(DEP_C_GAV), new String[][]{{DEP_B_GAV, DEP_A_GAV, MODULE_GAV}});
    }

    /**
     * Test module with a dependency that appear twice.
     */
    @Test
    public void twoParentsTest() {
        DependencyNode aDirect = createDependencyNode(DEP_A_GAV);
        DependencyNode b = createDependencyNode(DEP_B_GAV);
        DependencyNode aTransitive = createDependencyNode(DEP_A_GAV);
        module.setChildren(Lists.newArrayList(aDirect, b));
        b.setChildren(Lists.newArrayList(aTransitive));

        Map<String, String[][]> requirementMaps = dependencyResolutionSpy.createRequirementsMap(module);
        assertEquals(requirementMaps.size(), 2);
        assertEquals(requirementMaps.keySet(), Sets.newHashSet(DEP_A_GAV, DEP_B_GAV));
        comparePathToModule(requirementMaps.get(DEP_A_GAV), new String[][]{{MODULE_GAV}, {DEP_B_GAV, MODULE_GAV}});
        comparePathToModule(requirementMaps.get(DEP_B_GAV), new String[][]{{MODULE_GAV}});
    }

    private DependencyNode createDependencyNode(String gav) {
        return new DefaultDependencyNode(new DefaultArtifact(gav));
    }

    private void comparePathToModule(String[][] actual, String[][] expected) {
        assertTrue(Arrays.deepEquals(actual, expected),
                "Arrays not equal: " + Arrays.deepToString(expected) + " and " + Arrays.deepToString(actual) + ".");
    }

}
