package org.jfrog.build.extractor.clientConfiguration.util;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;

public class EncryptionUtils {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5PADDING";

    /**
     * Decrypts properties from an encrypted byte array using the provided secret key.
     *
     * @param encryptedData The encrypted byte array representing properties.
     * @param secretKey     The secret key used for decryption.
     * @return A Properties object containing the decrypted properties.
     */
    public static Properties decryptProperties(byte[] encryptedData, byte[] secretKey) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);

            String decryptedString = new String(cipher.doFinal(encryptedData));

            return stringToProperties(decryptedString);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | BadPaddingException | IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Properties stringToProperties(String propertiesString) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8))) {
            properties.load(inputStream);
        }
        return properties;
    }

    /**
     * Encrypts properties to a file represented as a byte array and returns the secret key used for encryption.
     *
     * @param os         The output stream where the encrypted properties will be written.
     * @param properties The Properties object containing the properties to be encrypted.
     * @return A byte array representing the secret key used for encryption.
     */
    public static byte[] encryptedPropertiesToFile(OutputStream os, Properties properties) throws IOException {
        byte[] secretKey = generateRandomKey();
        os.write(encryptProperties(properties, secretKey));
        return secretKey;
    }

    private static byte[] generateRandomKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[16];
        secureRandom.nextBytes(key);
        return key;
    }

    /**
     * Encrypts properties into a byte array using the provided secret key.
     *
     * @param properties The Properties object to be encrypted.
     * @param secretKey  The secret key used for encryption.
     * @return A byte array representing the encrypted properties.
     */
    private static byte[] encryptProperties(Properties properties, byte[] secretKey) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            String propertiesString = propertiesToString(properties);
            return cipher.doFinal(propertiesString.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | NoSuchAlgorithmException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private static String propertiesToString(Properties properties) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : properties.stringPropertyNames()) {
            stringBuilder.append(key).append("=").append(properties.getProperty(key)).append("\n");
        }
        return stringBuilder.toString();
    }
}
