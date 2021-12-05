package org.jfrog.gradle.plugin.artifactory.task;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.DistributerConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleClientLogger;

import java.io.IOException;
import java.util.ArrayList;

public class DistributeBuildTask extends DefaultTask {

    public static final String DISTRIBUTE_TASK_NAME = "artifactoryDistributeBuild";
    private static final Logger log = Logging.getLogger(DistributeBuildTask.class);

    @TaskAction
    public void distributeBuild() throws IOException {
        validate();

        ArtifactoryPluginConvention convention = (ArtifactoryPluginConvention) getProject().getConvention().getPlugins().get("artifactory");
        DistributerConfig distributerConfig = convention.getDistributerConfig();
        ArtifactoryClientConfiguration clientConf = convention.getClientConfig();

        String buildName = distributerConfig.getBuildName();
        String buildNumber = distributerConfig.getBuildNumber();
        buildName = buildName == null ? clientConf.info.getBuildName() : buildName;
        buildNumber = buildNumber == null ? clientConf.info.getBuildNumber() : buildNumber;

        Distribution distribution = new Distribution(distributerConfig.getPublish(),
                distributerConfig.getOverrideExistingFiles(),
                distributerConfig.getGpgPassphrase(),
                distributerConfig.getAsync(),
                distributerConfig.getTargetRepoKey(),
                new ArrayList<>(distributerConfig.getSourceRepoKeys()),
                distributerConfig.getDryRun());

        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(distributerConfig.getContextUrl(),
                distributerConfig.getUsername(), distributerConfig.getPassword(), new GradleClientLogger(getLogger()))) {
            artifactoryManager.distributeBuild(buildName, buildNumber, distribution);
            log.info(String.format("Successfully distributed build %s/%s", clientConf.info.getBuildName(), clientConf.info.getBuildNumber()));
        }
    }

    private void validate() {
        ArtifactoryPluginConvention convention = (ArtifactoryPluginConvention) getProject().getConvention().getPlugins().get("artifactory");
        DistributerConfig config = convention.getDistributerConfig();
        if (config == null) {
            throw new IllegalArgumentException("The build distribution configuration is missing.");
        }
        if (StringUtils.isEmpty(config.getContextUrl())) {
            throw new IllegalArgumentException("'contextUrl' is a mandatory field for build distribution.");
        }
        if (StringUtils.isEmpty(config.getTargetRepoKey())) {
            throw new IllegalArgumentException("'targetRepoKey' is a mandatory field for build distribution.");
        }
    }
}
