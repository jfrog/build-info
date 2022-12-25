package org.jfrog.build.extractor.scan;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.compress.utils.Sets;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author yahavi
 */
public class DependencyTreeTest {

    private DependencyTree root, one, two, three, four, five;

    /**
     * Build an empty tree with 5 nodes
     */
    @BeforeClass
    public void init() {
        root = new DependencyTree("0");
        one = new DependencyTree("1");
        two = new DependencyTree("2");
        three = new DependencyTree("3");
        four = new DependencyTree("4");
        five = new DependencyTree("5");
        root.add(one); // 0 -> 1
        root.add(two); // 0 -> 2
        two.add(three); // 2 -> 3
        two.add(four); // 2 -> 4
        four.add(five); // 4 -> 5
    }

    @Test
    public void testGetChildren() {
        // Check root children
        List<DependencyTree> rootChildren = root.getChildren();
        assertTrue(rootChildren.contains(one));
        assertTrue(rootChildren.contains(two));

        // Check 'one' children
        assertEquals(new ArrayList<>(), one.getChildren());

        // Check 'two' children
        List<DependencyTree> twoChildren = two.getChildren();
        assertTrue(twoChildren.contains(three));
        assertTrue(twoChildren.contains(four));

        // Check 'four' and 'five' children
        assertEquals(List.of(five), four.getChildren());
        assertEquals(new ArrayList<>(), five.getChildren());
    }

    @Test
    public void testInit() {
        // Sanity test - Check tree with no issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertTrue(rootIssues.isEmpty());
        Set<License> rootViolatedLicenses = root.processTreeViolatedLicenses();
        assertTrue(rootViolatedLicenses.isEmpty());
        assertEquals(Severity.Normal, root.getTopIssue().getSeverity());
    }

    @Test(dependsOnMethods = {"testInit"})
    public void testOneNode() {
        // Populate "1" with one empty issue and one empty license.
        Set<Issue> oneIssues = Sets.newHashSet(new Issue());
        Set<License> oneLicenses = Sets.newHashSet(new License());
        one.setIssues(oneIssues);
        one.setLicenses(oneLicenses);

        // Assert the tree has 1 issue
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(1, rootIssues.size());
        assertEquals(Severity.Normal, ((Issue) rootIssues.toArray()[0]).getSeverity());

        // Assert no violated licenses
        Set<License> rootViolatedLicenses = root.processTreeViolatedLicenses();
        assertTrue(rootViolatedLicenses.isEmpty());

        // Check isHigherSeverityThan() functionality
        assertTrue(createIssue(Severity.Unknown).isHigherSeverityThan(root.getTopIssue()));
    }

    @Test(dependsOnMethods = {"testOneNode"})
    public void testTwoNodes() {
        // Populate node two with one empty issue and one empty license.
        Set<Issue> twoIssues = Sets.newHashSet(createIssue(Severity.Normal));
        Set<License> twoLicenses = Sets.newHashSet(new License());
        two.setIssues(twoIssues);
        two.setLicenses(twoLicenses);

        // Assert the tree has 2 issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(2, rootIssues.size());
        assertEquals(Severity.Normal, ((Issue) rootIssues.toArray()[0]).getSeverity());
        assertEquals(Severity.Normal, ((Issue) rootIssues.toArray()[1]).getSeverity());
        assertTrue(createIssue(Severity.Unknown).isHigherSeverityThan(root.getTopIssue()));
    }

    @Test(dependsOnMethods = {"testTwoNodes"})
    public void testFourNodes() {
        // Populate node three with one Low issue
        Issue threeIssue = createIssue(Severity.Low);
        three.setIssues(Sets.newHashSet(threeIssue));
        three.setLicenses(Sets.newHashSet());
        three.processTreeIssues();

        // Assert the tree has 3 issues
        assertEquals(Severity.Low, three.getTopIssue().getSeverity());
        assertEquals("3", three.getTopIssue().getComponent());

        // Populate node four with Low and Medium issues
        Issue fourFirstIssue = createIssue(Severity.Medium);
        Issue fourSecondIssue = createIssue(Severity.Low);
        four.setIssues(Sets.newHashSet(fourFirstIssue, fourSecondIssue));

        // Assert the tree has 5 issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(5, rootIssues.size());
        assertEquals(fourFirstIssue, root.getTopIssue());
    }

    @Test(dependsOnMethods = {"testFourNodes"})
    public void testFiveNodes() {
        // Populate node five with 6 issues
        five.setIssues(Sets.newHashSet(createIssue(Severity.Normal),
                createIssue(Severity.Low),
                createIssue(Severity.Low),
                createIssue(Severity.Unknown),
                createIssue(Severity.Low),
                createIssue(Severity.High)));

        // Assert that all issues are in the tree
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(11, rootIssues.size());
        assertEquals(Severity.High, root.getTopIssue().getSeverity());
        assertEquals("5", root.getTopIssue().getComponent());
        assertEquals("", one.getTopIssue().getComponent());
        assertEquals("5", two.getTopIssue().getComponent());
        assertEquals("3", three.getTopIssue().getComponent());
        assertEquals("5", four.getTopIssue().getComponent());
        assertEquals("5", five.getTopIssue().getComponent());
    }

    @Test(dependsOnMethods = {"testFiveNodes"})
    public void testIsLicenseViolating() {
        assertTrue(root.getViolatedLicenses().isEmpty());

        License violatedLicense = createLicense(true);
        // Populate node three with 4 licenses, one violation
        three.setLicenses(Sets.newHashSet(createLicense(false),
                createLicense(false),
                createLicense(false),
                violatedLicense));
        // Populate node five with non violated license.
        five.setLicenses(Sets.newHashSet(createLicense(false)));

        // Assert that all licenses are in the tree
        Set<License> rootLicense = new HashSet<>();
        root.collectAllScopesAndLicenses(new HashSet<>(), rootLicense);
        assertEquals(6, rootLicense.size());

        Set<License> expectedViolatedLicenseSet = Sets.newHashSet(violatedLicense);
        Set<License> rootViolatedLicenses = root.processTreeViolatedLicenses();
        assertEquals(expectedViolatedLicenseSet, rootViolatedLicenses);
        assertTrue(one.getViolatedLicenses().isEmpty());
        assertEquals(expectedViolatedLicenseSet, two.getViolatedLicenses());
        assertEquals(expectedViolatedLicenseSet, three.getViolatedLicenses());
        assertTrue(three.getLicenses().contains(violatedLicense));
        assertTrue(four.getViolatedLicenses().isEmpty());
        assertTrue(five.getViolatedLicenses().isEmpty());
    }

    @Test
    public void testFixedVersions() {
        // Check no fixed versions
        Issue issue = createIssue(Severity.Normal);
        one.setIssues(Sets.newHashSet(issue));
        Set<Issue> rootIssues = root.processTreeIssues();
        assertEquals(Lists.newArrayList(), ((Issue) rootIssues.toArray()[0]).getFixedVersions());

        // Check null fixed versions
        issue.setFixedVersions(null);
        one.setIssues(Sets.newHashSet(issue));
        rootIssues = root.processTreeIssues();
        assertNull(((Issue) rootIssues.toArray()[0]).getFixedVersions());

        // Check one fixed version
        List<String> fixedVersions = Lists.newArrayList();
        fixedVersions.add("1.2.3");
        issue.setFixedVersions(fixedVersions);
        one.setIssues(Sets.newHashSet(issue));
        rootIssues = root.processTreeIssues();
        assertEquals(fixedVersions, ((Issue) rootIssues.toArray()[0]).getFixedVersions());

        // Check two fixed version
        fixedVersions.add("1.3.2");
        issue.setFixedVersions(fixedVersions);
        one.setIssues(Sets.newHashSet(issue));
        rootIssues = root.processTreeIssues();
        assertEquals(fixedVersions, ((Issue) rootIssues.toArray()[0]).getFixedVersions());

        // Clean up
        root.setIssues(Sets.newHashSet());
        one.setIssues(Sets.newHashSet());
    }

    @Test
    public void testFind() {
        assertNotNull(root.find("0"));
        assertNotNull(root.find("1"));
        assertNotNull(root.find("2"));
        assertNotNull(root.find("3"));
        assertNotNull(root.find("4"));
        assertNotNull(root.find("5"));
        assertNull(root.find("non-existent"));
    }

    @Test
    public void testHasLoop() {
        // Make sure the test dependency doesn't have loops
        assertFalse(root.hasLoop(new NullLog()));

        // Build a dependency tree with a loop: 1 -> 2 -> 3 -> 1
        DependencyTree one = new DependencyTree("1");
        DependencyTree two = new DependencyTree("2");
        DependencyTree three = new DependencyTree("3");
        DependencyTree anotherOne = new DependencyTree("1");
        one.add(two);
        two.add(three);
        three.add(anotherOne);

        // Make sure the dependency tree has a loop
        assertTrue(anotherOne.hasLoop(new NullLog()));
    }

    /**
     * Create a random issue
     *
     * @param severity the issue severity
     * @return the random issue
     */
    private Issue createIssue(Severity severity) {
        return new Issue(generateUID(), severity, generateUID(), Lists.newArrayList(),
                Lists.newArrayList(), Lists.newArrayList(), generateUID());
    }

    /**
     * Create a random license
     *
     * @param violating a boolean indicates if the licenses is violating a policy.
     * @return the random issue
     */
    private License createLicense(boolean violating) {
        return new License(generateUID(), generateUID(), Lists.newArrayList(), violating);
    }

    private String generateUID() {
        return UUID.randomUUID().toString();
    }
}
