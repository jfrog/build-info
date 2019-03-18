package org.jfrog.build.extractor.scan;

import org.apache.commons.compress.utils.Sets;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Set;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author yahavi
 */
public class DependenciesTreeTest extends DependenciesTreeTestBase {

    @BeforeClass
    public void init() {
        super.init();
    }

    @Test
    public void testInit() {
        // Sanity test - Check tree with no issues
        Set<Issue> rootIssues = root.processTreeIssues();
        assertTrue(rootIssues.isEmpty());
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
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[0]).getSeverity());

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
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[0]).getSeverity());
        assertEquals(Severity.Normal, ((Issue)rootIssues.toArray()[1]).getSeverity());
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

}