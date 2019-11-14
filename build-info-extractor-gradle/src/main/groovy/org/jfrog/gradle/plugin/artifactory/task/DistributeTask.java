package org.jfrog.gradle.plugin.artifactory.task;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.util.EntityUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.DistributerConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger;
import java.io.IOException;
import java.util.ArrayList;

public class DistributeTask extends DefaultTask {

    public static final String DISTRIBUTE_TASK_NAME = "artifactoryDistribute";
    private static final Logger log = Logging.getLogger(DistributeTask.class);

    @TaskAction
    public void distributeBuild() throws IOException {
        validate();

        ArtifactoryPluginConvention convention = (ArtifactoryPluginConvention) getProject().getConvention().getPlugins().get("artifactory");
        DistributerConfig distributerConfig = convention.getDistributerConfig();
        ArtifactoryClientConfiguration clientConf = convention.getClientConfig();

        ArtifactoryBuildInfoClient infoClient = new ArtifactoryBuildInfoClient(
                distributerConfig.getContextUrl(),
                distributerConfig.getUsername(),
                distributerConfig.getPassword(),
                new GradleClientLogger(getLogger()));

        HttpResponse response = infoClient.distributeBuild(
                clientConf.info.getBuildName(),
                clientConf.info.getBuildNumber(),
                new Distribution(distributerConfig.getPublish(),
                        distributerConfig.getOverrideExistingFiles(),
                        distributerConfig.getGpgPassphrase(),
                        distributerConfig.getAsync(),
                        distributerConfig.getTargetRepoKey(),
                        new ArrayList<>(distributerConfig.getSourceRepoKeys()),
                        distributerConfig.getDryRun()));

        String content;
        if (response.getEntity() != null) {
            content = EntityUtils.toString(response.getEntity(), "UTF-8");
        } else {
            content = "";
        }

        StatusLine status = response.getStatusLine();
        if (status.getStatusCode() != 200) {
            throw new IOException(String.format("Distribution failed. Received '%s', '%s' from Artifactory.", status.getReasonPhrase(), content));
        }

        log.info(String.format("Successfully distributed build %s/%s", clientConf.info.getBuildName(), clientConf.info.getBuildNumber()));
    }

    private void validate() {
        ArtifactoryPluginConvention convention = (ArtifactoryPluginConvention)getProject().getConvention().getPlugins().get("artifactory");
        DistributerConfig config = convention.getDistributerConfig();
        if (StringUtils.isEmpty(config.getContextUrl())) {
            throw new IllegalArgumentException("'contextUrl' is a mandatory field for build distribution");
        }
        if (StringUtils.isEmpty(config.getTargetRepoKey())) {
            throw new IllegalArgumentException("'targetRepoKey' is a mandatory field for build distribution");
        }
    }
}