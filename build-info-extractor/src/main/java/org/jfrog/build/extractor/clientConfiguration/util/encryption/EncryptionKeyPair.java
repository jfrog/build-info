package org.jfrog.build.extractor.clientConfiguration.util.encryption;

import org.apache.commons.lang3.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Represents a pair of secret key and initialization vector (IV) used for encryption and decryption.
 */
public class EncryptionKeyPair {
    private static final int AES_256_KEY_LENGTH = 256;
    private static final int IV_LENGTH = 128;
    private byte[] secretKey;
    private byte[] iv;

    public EncryptionKeyPair() {
        this.secretKey = generateRandomKey(AES_256_KEY_LENGTH);
        this.iv = generateRandomKey(IV_LENGTH);
    }

    public EncryptionKeyPair(String secretKey, String Iv) {
        if (StringUtils.isNotBlank(secretKey)) {
            this.secretKey = Base64.getDecoder().decode(secretKey);
        }
        if (StringUtils.isNotBlank(Iv)) {
            this.iv = Base64.getDecoder().decode(Iv);
        }
    }

    /**
     * Generates a random key of the specified length in bits.
     *
     * @param lengthInBits The length of the key in bits.
     * @return A byte array representing the generated random key.
     */
    private static byte[] generateRandomKey(int lengthInBits) {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[lengthInBits / 8];
        secureRandom.nextBytes(key);
        return key;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    @SuppressWarnings("unused")
    public String getStringSecretKey() {
        return Base64.getEncoder().encodeToString(secretKey);
    }

    public byte[] getIv() {
        return iv;
    }

    @SuppressWarnings("unused")
    public String getStringIv() {
        return Base64.getEncoder().encodeToString(iv);
    }

    public boolean isEmpty() {
        return secretKey == null || secretKey.length == 0 || iv == null || iv.length == 0;
    }
}
