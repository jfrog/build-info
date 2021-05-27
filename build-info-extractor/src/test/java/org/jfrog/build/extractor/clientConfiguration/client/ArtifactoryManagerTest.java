package org.jfrog.build.extractor.clientConfiguration.client;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.jfrog.build.extractor.clientConfiguration.util.JsonUtils.toJsonString;

/**
 * Integration tests for the ArtifactoryBuildInfoClient.
 * Performs tests using the infrastructure resources provided by IntegrationTestsBase (such as Artifactory, localRepo, and credentials).
 *
 * @author Yahav Itzhak
 */
@Test
public class ArtifactoryManagerTest extends IntegrationTestsBase {
    private static final String TEST_SPACE = "bi_client_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private static final String BUILD_NAME = "ArtifactoryManagerTest";
    private static final String BUILD_NUMBER = "13";

    @BeforeMethod
    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
        deleteContentFromRepo(localRepo1);
    }

    /**
     * Send build info to artifactory, receive it and compare.
     */
    @Test
    public void sendBuildInfoAndBuildRetentioTest() throws IOException {
        doSendBuildInfoTest(null);
        sendBuildRetentionTest("");
    }

    @Test
    public void sendBuildInfoWithProjectTest() throws IOException {
        doSendBuildInfoTest("jit");
        sendBuildRetentionTest("jit");
    }

    private void doSendBuildInfoTest(String project) throws IOException {
        final Date STARTED = new Date();
        final List<Vcs> VCS = Arrays.asList(new Vcs("foo", "1"),
                new Vcs("bar", "2"),
                new Vcs("baz", "3"));
        final List<MatrixParameter> RUN_PARAMETERS = Arrays.asList(new MatrixParameter("a", "b"), new MatrixParameter("c", "d"));
        final Module module = new Module();
        module.setId("foo");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        final List<PromotionStatus> STATUSES = Arrays.asList(new PromotionStatus("a", "b", "c", simpleDateFormat.format(STARTED), "e", "f"));

        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(BUILD_NAME)
                .number(BUILD_NUMBER)
                .setProject(project)
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
                .modules(Collections.singletonList(module))
                .statuses(STATUSES)
                .issues(new Issues());
        Build buildInfoToSend = buildInfoBuilder.build();

        // Publish build info
        artifactoryManager.publishBuildInfo(buildInfoToSend, project);

        // Get build info
        Build receivedBuildInfo = artifactoryManager.getBuildInfo(BUILD_NAME, BUILD_NUMBER, project);

        // Compare
        Assert.assertEquals(toJsonString(buildInfoToSend), toJsonString(receivedBuildInfo));
    }

    @Test
    private void sendBuildRetentionTest(String project) throws IOException {
        BuildRetention buildRetention = new BuildRetention();
        buildRetention.setCount(1);
        buildRetention.setDeleteBuildArtifacts(false);
        artifactoryManager.sendBuildRetention(buildRetention, BUILD_NAME, project, false);
    }
}
