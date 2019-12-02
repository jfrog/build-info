package org.jfrog.build.extractor.usageReport;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import java.io.IOException;

/**
 * Created by Bar Belity on 20/11/2019.
 */
public class UsageReporter {
    private String productId;
    private FeatureId[] features;
    private Log log;
    private ArtifactoryBuildInfoClient client;

    public UsageReporter(String productId, String[] featureIds, String artifactoryUrl, String username, String password, String accessToken, Log log) {
        this.productId = productId;
        this.log = log;
        setFeatures(featureIds);
        this.client = new ArtifactoryBuildInfoClient(artifactoryUrl, username, password, accessToken, log);
    }

    public UsageReporter(String productId, String[] featureIds, String artifactoryUrl, String username, String password, String accessToken, Log log, ProxyConfiguration proxyConfiguration) {
        this(productId, featureIds, artifactoryUrl, username, password, accessToken, log);
        client.setProxyConfiguration(proxyConfiguration);
    }

    public void reportUsage() {
        try {
            client.reportUsage(this);
        } catch (IOException ex) {
            log.info("Failed reporting usage to Artifactory: " + ex);
        }
    }

    public String getProductId() {
        return productId;
    }

    public FeatureId[] getFeatures() {
        return features;
    }

    private void setFeatures(String[] featureIds) {
        features = new FeatureId[featureIds.length];
        int featureIndex = 0;
        for (String featureId : featureIds) {
            features[featureIndex] = new FeatureId(featureId);
            featureIndex++;
        }
    }

    public class FeatureId {
        private String featureId;

        public FeatureId (String featureId) {
            this.featureId = featureId;
        }

        public String getFeatureId() {
            return featureId;
        }
    }
}
