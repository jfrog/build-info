package org.jfrog.build.extractor.usageReport;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
    private String uniqueClientId;

    public UsageReporter(String productId, String[] featureIds) {
        this.productId = productId;
        setFeatures(featureIds);
    }

    @SuppressWarnings("unused")
    public void reportUsage(String artifactoryUrl, String username, String password, String accessToken, ProxyConfiguration proxyConfiguration, Log log) throws IOException {
        try (ArtifactoryManager artifactoryManager = new ArtifactoryManager(artifactoryUrl, username, password, accessToken, log)) {
            if (proxyConfiguration != null) {
                artifactoryManager.setProxyConfiguration(proxyConfiguration);
            }
            try {
                uniqueClientId = uniqueClientId == null ? generateUniqueClientId() : uniqueClientId;
            } catch (SocketException e) {
                log.warn("Wasn't able to generate unique client ID: " + ExceptionUtils.getRootCauseMessage(e));
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

    /**
     * Generates unique client ID based on MAC address.
     * The MAC address can't be retrieved from the generated ID, as SHA1 hashing is used.
     *
     * @return unique client ID
     * @throws SocketException if an I/O error occurs while trying to found the MAC address
     */
    private String generateUniqueClientId() throws SocketException {
        byte[] macAddress = getMacAddress();
        return macAddress != null ? DigestUtils.sha1Hex(macAddress) : null;
    }

    private static byte[] getMacAddress() throws SocketException {
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface networkInterface = networkInterfaces.nextElement();
            byte[] macAddressed = networkInterface.getHardwareAddress();
            if (macAddressed != null) {
                return macAddressed;
            }
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
