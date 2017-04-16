package org.jfrog.build.extractor.retention;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildRetention;
import org.jfrog.build.client.ArtifactoryHttpClient;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.util.VersionException;

import java.io.IOException;
import java.util.Calendar;

import static org.jfrog.build.client.ArtifactoryHttpClient.encodeUrl;

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

    private static void addRetentionIfNeeded(Build build, BuildRetention retention, ArtifactoryVersion version) {
        if (!version.isAtLeast(ArtifactoryHttpClient.STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION)) {
            build.setBuildRetention(retention);
        }
    }

    private static void sendRetentionIfNeeded(ArtifactoryBuildInfoClient client, BuildRetention retention, String buildName, ArtifactoryVersion version) throws IOException {
        if (version.isAtLeast(ArtifactoryHttpClient.STANDALONE_BUILD_RETENTION_SUPPORTED_ARTIFACTORY_VERSION)) {
            client.sendBuildRetetion(retention, encodeUrl(buildName));
        }
    }

    public static void sendBuildAndBuildRetention(ArtifactoryBuildInfoClient client, Build build, ArtifactoryClientConfiguration clientConf) throws IOException {
        BuildRetention retention = getBuildRetention(clientConf);
        sendBuildAndBuildRetention(client, build, retention);
    }

    public static void sendBuildAndBuildRetention(ArtifactoryBuildInfoClient client, Build build, BuildRetention retention) throws IOException {
        if (retention.isEmpty()) {
            client.sendBuildInfo(build);
            return;
        }
        ArtifactoryVersion version;
        try {
            version = client.verifyCompatibleArtifactoryVersion();
        } catch (VersionException e) {
            throw new RuntimeException(e);
        }
        addRetentionIfNeeded(build, retention, version);
        client.sendBuildInfo(build);
        sendRetentionIfNeeded(client, retention, build.getName(), version);
    }
}
