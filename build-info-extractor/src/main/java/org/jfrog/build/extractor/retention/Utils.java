package org.jfrog.build.extractor.retention;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.ci.BuildRetention;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.JFrogHttpClient;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;
import java.util.Calendar;


/**
 * Created by tamirh on 13/04/2017.
 */
public class Utils {

    private static BuildRetention getBuildRetention(ArtifactoryClientConfiguration clientConf) {
        BuildRetention buildRetention = new BuildRetention(clientConf.info.isDeleteBuildArtifacts());
        if (clientConf.info.getBuildRetentionCount() != null) {
            buildRetention.setCount(clientConf.info.getBuildRetentionCount());
        }
        String buildRetentionMinimumDays = clientConf.info.getBuildRetentionMinimumDate();
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays);
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, -minimumDays);
                buildRetention.setMinimumBuildDate(calendar.getTime());
            }
        }
        String[] notToDelete = clientConf.info.getBuildNumbersNotToDelete();
        for (String notToDel : notToDelete) {
            buildRetention.addBuildNotToBeDiscarded(notToDel);
        }

        return buildRetention;
    }

    private static void addRetentionIfNeeded(BuildInfo buildInfo, BuildRetention retention, ArtifactoryVersion version) {
        if (!version.isAtLeast(JFrogHttpClient.STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION)) {
            buildInfo.setBuildRetention(retention);
        }
    }

    private static void sendRetentionIfNeeded(ArtifactoryManager artifactoryManager, BuildRetention retention, String buildName, String project, ArtifactoryVersion version, boolean async) throws IOException {
        if (version.isAtLeast(JFrogHttpClient.STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION)) {
            artifactoryManager.sendBuildRetention(retention, buildName, project, async);
        }
    }

    public static void sendBuildAndBuildRetention(ArtifactoryManager artifactoryManager, BuildInfo buildInfo, ArtifactoryClientConfiguration clientConf, String platformUrl) throws IOException {
        BuildRetention retention = getBuildRetention(clientConf);
        sendBuildAndBuildRetention(artifactoryManager, buildInfo, retention, clientConf.info.isAsyncBuildRetention(), platformUrl);
    }

    public static void sendBuildAndBuildRetention(ArtifactoryManager artifactoryManager, BuildInfo buildInfo, ArtifactoryClientConfiguration clientConfl) throws IOException {
        sendBuildAndBuildRetention(artifactoryManager, buildInfo, clientConfl, null);
    }

    public static void sendBuildAndBuildRetention(ArtifactoryManager artifactoryManager, BuildInfo buildInfo, BuildRetention retention, boolean asyncBuildRetention, String platformUrl) throws IOException {
        if (retention == null || retention.isEmpty()) {
            artifactoryManager.publishBuildInfo(buildInfo, platformUrl);
            return;
        }
        ArtifactoryVersion version;
        version = artifactoryManager.getVersion();
        addRetentionIfNeeded(buildInfo, retention, version);
        artifactoryManager.publishBuildInfo(buildInfo, platformUrl);
        sendRetentionIfNeeded(artifactoryManager, retention, buildInfo.getName(), buildInfo.getProject(), version, asyncBuildRetention);
    }

    public static void sendBuildAndBuildRetention(ArtifactoryManager artifactoryManager, BuildInfo buildInfo, BuildRetention retention, boolean asyncBuildRetention) throws IOException {
        sendBuildAndBuildRetention(artifactoryManager, buildInfo, retention, asyncBuildRetention, null);
    }
}
