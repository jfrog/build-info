package org.jfrog.build.extractor.issuesCollection;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
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
        String successfulConfig = FileUtils.readFileToString(new File(testResourcesPath, path));
        return collector.parseConfig(successfulConfig);
    }

    private void verifySuccessfulConfig(IssuesCollectionConfig parsedConfig) {
        Assert.assertNotNull(parsedConfig);
        Assert.assertEquals(parsedConfig.getVersion(), 1);
        IssuesCollectionConfig.Issues issues = parsedConfig.getIssues();
        Assert.assertEquals(issues.getTrackerName(), "TESTING");
        // Double backslash since the parseConfig method doubles them as well:
        Assert.assertEquals(issues.getRegexp(), "([a-zA-Z]+-[0-9]*)\\\\s-\\\\s(.*)");
        Assert.assertEquals(issues.getKeyGroupIndex(), 1);
        Assert.assertEquals(issues.getSummaryGroupIndex(), 2);
        Assert.assertEquals(issues.getTrackerUrl(), "http://TESTING.com");
        Assert.assertTrue(issues.isAggregate());
        Assert.assertEquals(issues.getAggregationStatus(), "RELEASE");
    }

    @Test
    public void testDoCollectWithoutRevision() throws IOException, InterruptedException {
        runDoCollect("git_issues_.git_suffix", new Vcs(), 2);
    }

    @Test
    public void testDoCollectWithRevision() throws IOException, InterruptedException {
        runDoCollect("git_issues2_.git_suffix", new Vcs("http://TESTING.com", "6198a6294722fdc75a570aac505784d2ec0d1818"), 2);
    }

    private void runDoCollect(String sourceFolder, Vcs vcs, int expectedNumOfIssues) throws IOException, InterruptedException {
        // Copy the provided folder and create .git
        FileUtils.copyDirectory(new File(testResourcesPath, "git_issues_.git_suffix"), dotGitPath);

        // Publishing build without vcs
        publishBuildInfoWithVcs(null);

        // Get config
        String successfulConfig = FileUtils.readFileToString(new File(testResourcesPath, "issues_config_full_test.json"));

        Issues issues = collector.collectIssues(dotGitPath, getLog(), successfulConfig, buildInfoClient, BUILD_NAME);

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
        buildInfoClient.sendBuildInfo(buildInfoToSend);
    }

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        deleteContentFromRepo(localRepo);
        FileUtils.deleteDirectory(dotGitPath);
    }
}
