package org.jfrog.build.extractor.clientConfiguration.client;

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.release.PromotionStatus;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Integration tests for the ArtifactoryBuildInfoClient.
 * Performs tests using the resources provided by IntegrationTestsBase (such as Artifactory, localRepo, and credentials).
 *
 * @author Yahav Itzhak
 */
@Test
public class ArtifactoryBuildInfoClientTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_client_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo);
    }

    /**
     * Send build info to artifactory, receive it and compare.
     */
    @Test
    public void sendBuildInfoTest() throws IOException {
        final String BUILD_NAME = "ArtifactoryBuildInfoClientTest";
        final String BUILD_NUMBER = "13";
        final Date STARTED = new Date();
        final List<Vcs> VCS = Lists.newArrayList(new Vcs("foo", "1"),
                new Vcs("bar", "2"),
                new Vcs("baz", "3"));
        final List<MatrixParameter> RUN_PARAMETERS = Lists.newArrayList(new MatrixParameter("a", "b"), new MatrixParameter("c", "d"));
        final Module module = new Module();
        module.setId("foo");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        final List<PromotionStatus> STATUSES = Lists.newArrayList(new PromotionStatus("a", "b", "c", simpleDateFormat.format(STARTED), "e", "f"));

        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(BUILD_NAME)
                .number(BUILD_NUMBER)
                .buildAgent(new BuildAgent("agent11", "11"))
                .agent(new Agent("agent22", "22"))
                .startedDate(STARTED)
                .durationMillis(100)
                .principal("foo")
                .artifactoryPrincipal(getUsername())
                .artifactoryPluginVersion("2.3.1")
                .url(getUrl())
                .parentName("baz")
                .parentNumber("10")
                .vcs(VCS)
                .licenseControl(new LicenseControl(false))
                .buildRetention(new BuildRetention(true))
                .buildRunParameters(RUN_PARAMETERS)
                .modules(Lists.newArrayList(module))
                .statuses(STATUSES)
                .issues(new Issues());
        Build buildInfoToSend = buildInfoBuilder.build();

        // Publish build info
        buildInfoClient.sendBuildInfo(buildInfoToSend);

        // Get build info
        Build receivedBuildInfo = buildInfoClient.getBuildInfo(BUILD_NAME, BUILD_NUMBER);

        // Compare
        Assert.assertEquals(buildInfoClient.toJsonString(buildInfoToSend), buildInfoClient.toJsonString(receivedBuildInfo));
    }
}
