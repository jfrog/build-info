package org.jfrog.build.extractor.usageReport;

import org.apache.commons.codec.digest.DigestUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ProxyConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by Bar Belity on 20/11/2019.
 */
public class UsageReporter {
    private final String productId;
    private FeatureId[] features;
    private final String uniqueClientId;

    public UsageReporter(String productId, String[] featureIds) {
        this.productId = productId;
        setFeatures(featureIds);
        this.uniqueClientId = generateUniqueClientId();
    }

    private String generateUniqueClientId() {
        byte[] macAddress = getMacAddress();
        if (macAddress != null) {
            return new String(DigestUtils.sha1(macAddress));
        }
        return null;
    }

    @SuppressWarnings("unused")
    public void reportUsage(String artifactoryUrl, String username, String password, String accessToken, ProxyConfiguration proxyConfiguration, Log log) throws IOException {
        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(artifactoryUrl, username, password, accessToken, log)) {
            if (proxyConfiguration != null) {
                artifactoryManager.setProxyConfiguration(proxyConfiguration);
            }
            artifactoryManager.reportUsage(this);
        }
    }

    @SuppressWarnings("unused")
    public String getProductId() {
        return productId;
    }

    @SuppressWarnings("unused")
    public FeatureId[] getFeatures() {
        return features;
    }

    @SuppressWarnings("unused")
    public String getUniqueClientId() {
        return uniqueClientId;
    }

    private void setFeatures(String[] featureIds) {
        features = new FeatureId[featureIds.length];
        int featureIndex = 0;
        for (String featureId : featureIds) {
            features[featureIndex] = new FeatureId(featureId);
            featureIndex++;
        }
    }

    private static byte[] getMacAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (!networkInterfaces.hasMoreElements()) {
                return null;
            }
            for (NetworkInterface networkInterface = networkInterfaces.nextElement(); networkInterfaces.hasMoreElements(); networkInterface = networkInterfaces.nextElement()) {
                byte[] macAddressed = networkInterface.getHardwareAddress();
                if (macAddressed != null) {
                    return macAddressed;
                }
            }
        } catch (SocketException e) {
            return null;
        }
        return null;
    }

    public static class FeatureId {
        private final String featureId;

        public FeatureId(String featureId) {
            this.featureId = featureId;
        }

        @SuppressWarnings("unused")
        public String getFeatureId() {
            return featureId;
        }
    }
}
