package org.jfrog.build.extractor.clientConfiguration.util.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class SecurePropertiesEncryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;



    /**
     * Decrypts properties from an encrypted byte array using the provided secret key.
     *
     * @param encryptedData The encrypted byte array representing properties.
     * @param keyPair       The secret key and iv used for decryption.
     * @return A Properties object containing the decrypted properties.
     */
    public static Properties decryptProperties(byte[] encryptedData, EncryptionKeyPair keyPair) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyPair.getSecretKey(), ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, keyPair.getIv());

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);

            byte[] decryptedBytes = cipher.doFinal(encryptedData);
            String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);

            return stringToProperties(decryptedString);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
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
    public static EncryptionKeyPair encryptedPropertiesToFile(OutputStream os, Properties properties) throws IOException {
        EncryptionKeyPair keyPair = new EncryptionKeyPair();
        os.write(encryptProperties(properties, keyPair));
        return keyPair;
    }



    /**
     * Encrypts properties into a byte array using the provided secret key.
     *
     * @param properties The Properties object to be encrypted.
     * @param keyPair    The secret key and iv used for encryption.
     * @return A byte array representing the encrypted properties.
     */
    private static byte[] encryptProperties(Properties properties, EncryptionKeyPair keyPair) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyPair.getSecretKey(), ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, keyPair.getIv());

            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);

            String propertiesString = propertiesToString(properties);
            return cipher.doFinal(propertiesString.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                 | BadPaddingException | IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
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
