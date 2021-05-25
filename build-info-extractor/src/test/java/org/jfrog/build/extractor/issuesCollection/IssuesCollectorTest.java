package org.jfrog.build.extractor.issuesCollection;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Issues;
import org.jfrog.build.api.IssuesCollectionConfig;
import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class IssuesCollectorTest extends IntegrationTestsBase {
    private static final String BASE_CONFIG_PATH = "/issuesCollection";
    private static final String BUILD_NAME = "IssuesCollectionTest";
    private IssuesCollector collector;
    private File testResourcesPath;
    private File dotGitPath;


    @BeforeClass
    public void initIssuesCollection() throws URISyntaxException, IOException {
        collector = new IssuesCollector();
        testResourcesPath = new File(this.getClass().getResource(BASE_CONFIG_PATH).toURI()).getCanonicalFile();
        dotGitPath = new File(testResourcesPath, ".git");
    }

    @Test
    public void testIssuesParseConfig() throws IOException {
        // Test success scenario
        IssuesCollectionConfig parsedConfig = parseConfigFromPath(collector, "issues_config_success.json");
        verifySuccessfulConfig(parsedConfig);

        // Test fail scenarios
        String[] failConfigPaths = new String[]{
                "issues_config_fail_no_issues.json",
                "issues_config_fail_invalid_group_index.json",
                "issues_config_fail_invalid_aggregate.json"
        };

        for (String path : failConfigPaths) {
            try {
                IssuesCollectionConfig failConfig = parseConfigFromPath(collector, path);
                // If didn't fail:
                Assert.fail("Parsing config should have failed for path: " + path);
            } catch (IOException e) {
                // Expected IOException
                getLog().info("-----\n Got an expected IOException from path: "+ path);
                getLog().info("Exception message:");
                getLog().info(e.getMessage());
            }
        }
    }

    private IssuesCollectionConfig parseConfigFromPath(IssuesCollector collector, String path) throws IOException {
        String successfulConfig = FileUtils.readFileToString(new File(testResourcesPath, path), "UTF-8");
        return collector.parseConfig(successfulConfig);
    }

    private void verifySuccessfulConfig(IssuesCollectionConfig parsedConfig) {
        Assert.assertNotNull(parsedConfig);
        Assert.assertEquals(parsedConfig.getVersion(), 1);
        IssuesCollectionConfig.Issues issues = parsedConfig.getIssues();
        Assert.assertEquals(issues.getTrackerName(), "TESTING");
        Assert.assertEquals(issues.getRegexp(), "([a-zA-Z]+-[0-9]*)\\s-\\s(.*)");
        Assert.assertEquals(issues.getKeyGroupIndex(), 1);
        Assert.assertEquals(issues.getSummaryGroupIndex(), 2);
        Assert.assertEquals(issues.getTrackerUrl(), "http://TESTING.com");
        Assert.assertTrue(issues.isAggregate());
        Assert.assertEquals(issues.getAggregationStatus(), "RELEASE");
    }

    @Test
    public void testCollectIssuesWithoutRevision() throws IOException, InterruptedException {
        List<Vcs> vcsList = Collections.singletonList(new Vcs());
        runCollectIssues("git_issues_.git_suffix", vcsList, 2);
    }

    @Test
    public void testCollectIssuesWithRevision() throws IOException, InterruptedException {
        List<Vcs> vcsList = Collections.singletonList(new Vcs("http://TESTING.com", "6198a6294722fdc75a570aac505784d2ec0d1818"));
        runCollectIssues("git_issues2_.git_suffix", vcsList, 2);
    }

    @Test
    /**
     * Test collection with a made up revision - the command should not throw an error, and 0 issues should be returned.
     * This test covers the cenario of missing revision on the 'git log' output, probably due to a squash / revert. therefore, ignore and don't collect new issues.
     */
    public void testCollectIssuesWithNonExistingRevision() throws IOException, InterruptedException {
        List<Vcs> vcsList = Collections.singletonList(new Vcs("http://TESTING.com", "abcdefABCDEF1234567890123456789012345678"));
        runCollectIssues("git_issues2_.git_suffix", vcsList, 0);
    }

    private void runCollectIssues(String sourceFolder, List<Vcs> vcs, int expectedNumOfIssues) throws IOException, InterruptedException {
        // Copy the provided folder and create .git
        FileUtils.deleteDirectory(dotGitPath);
        FileUtils.copyDirectory(new File(testResourcesPath, sourceFolder), dotGitPath);

        // Publishing build without vcs
        publishBuildInfoWithVcs(vcs);

        // Get config
        String successfulConfig = FileUtils.readFileToString(new File(testResourcesPath, "issues_config_full_test.json"), "UTF-8");

        Issues issues = collector.collectIssues(dotGitPath, getLog(), successfulConfig, artifactoryManagerBuilder, BUILD_NAME, vcs.get(0), null);

        Assert.assertNotNull(issues);
        Assert.assertNotNull(issues.getAffectedIssues());
        Assert.assertEquals(expectedNumOfIssues, issues.getAffectedIssues().size());
    }

    private void publishBuildInfoWithVcs(List<Vcs> vcsList) throws IOException {
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(BUILD_NAME)
                .number("123")
                .startedDate(new Date())
                .url(getUrl())
                .vcs(vcsList);
        Build buildInfoToSend = buildInfoBuilder.build();

        // Publish build info
        artifactoryManager.publishBuildInfo(buildInfoToSend, null);
    }

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        deleteContentFromRepo(localRepo1);
        FileUtils.deleteDirectory(dotGitPath);
    }
}
