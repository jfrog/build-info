package org.jfrog.build.extractor.clientConfiguration.util.encryption;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import static org.jfrog.build.extractor.clientConfiguration.util.encryption.Encryptor.decrypt;
import static org.jfrog.build.extractor.clientConfiguration.util.encryption.Encryptor.encrypt;

public class PropertyEncryptor {

    /**
     * Decrypts properties from a file using the provided secret key.
     *
     * @param filePath The path to the file containing encrypted properties.
     * @param keyPair  The secret key and iv used for decryption.
     * @return A Properties object containing the decrypted properties.
     */
    public static Properties decryptPropertiesFromFile(String filePath, EncryptionKeyPair keyPair) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }
        if (!new File(filePath).exists()) {
            throw new IOException("File " + filePath + " does not exist");
        }
        return decryptProperties(FileUtils.readFileToByteArray(new File(filePath)), keyPair);
    }

    /**
     * Decrypts properties from an encrypted byte array using the provided secret key.
     *
     * @param encryptedData The encrypted byte array representing properties.
     * @param keyPair       The secret key and iv used for decryption.
     * @return A Properties object containing the decrypted properties.
     */
    private static Properties decryptProperties(byte[] encryptedData, EncryptionKeyPair keyPair) throws IOException, InvalidAlgorithmParameterException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, NoSuchAlgorithmException, InvalidKeyException {
        byte[] decryptedBytes = decrypt(encryptedData, keyPair);
        String decryptedString = new String(decryptedBytes, StandardCharsets.UTF_8);
        // Escape backslashes so that they won't be removed when loading the properties.
        decryptedString = decryptedString.replace("\\", "\\\\");
        return stringToProperties(decryptedString);
    }

    /**
     * Encrypts properties to a file represented as a byte array and returns the secret key used for encryption.
     *
     * @param os         The output stream where the encrypted properties will be written.
     * @param properties The Properties object containing the properties to be encrypted.
     * @return A byte array representing the secret key used for encryption.
     */
    public static EncryptionKeyPair encryptedPropertiesToStream(OutputStream os, Properties properties) throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        EncryptionKeyPair keyPair = new EncryptionKeyPair();
        os.write(encrypt(propertiesToString(properties).getBytes(StandardCharsets.UTF_8), keyPair));
        return keyPair;
    }

    private static Properties stringToProperties(String propertiesString) throws IOException {
        Properties properties = new Properties();
        try (InputStream inputStream = new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8))) {
            properties.load(inputStream);
        }
        return properties;
    }

    private static String propertiesToString(Properties properties) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String key : properties.stringPropertyNames()) {
            stringBuilder.append(key).append("=").append(properties.getProperty(key)).append("\n");
        }
        return stringBuilder.toString();
    }
}
