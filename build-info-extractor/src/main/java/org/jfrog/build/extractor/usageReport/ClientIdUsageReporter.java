package org.jfrog.build.extractor.usageReport;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.Log;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by tala on 02/04/2023.
 */
@SuppressWarnings("unused")
public class ClientIdUsageReporter extends UsageReporter {
    private final Log logger;
    private final String uniqueClientId;

    @SuppressWarnings("unused")
    public String getUniqueClientId() {
        return uniqueClientId;
    }

    @SuppressWarnings("unused")
    public ClientIdUsageReporter(String productId, String[] featureIds, Log logger) {
        super(productId, featureIds);
        this.logger = logger;
        uniqueClientId = generateUniqueClientId();
    }

    /**
     * Generates unique client ID based on MAC address.
     * The MAC address can't be retrieved from the generated ID, as SHA1 hashing is used.
     *
     * @return unique client ID
     */
    private String generateUniqueClientId() {
        byte[] macAddress = null;
        try {
            macAddress = getMacAddress();
        } catch (SocketException e) {
            logger.warn("Wasn't able to generate unique client ID: " + ExceptionUtils.getRootCauseMessage(e));
        }
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
}
