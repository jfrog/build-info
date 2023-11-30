package org.jfrog.build.extractor.util.encryption;

import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.SecurePropertiesEncryption;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class SecurePropertiesEncryptionTest {
    @Test
    public void testEncryptDecryptProperties() throws IOException {
        // Create properties to be encrypted
        Properties originalProperties = new Properties();
        originalProperties.setProperty("key1", "value1");
        originalProperties.setProperty("key2", "value2");

        // Encrypt properties and get encryption key pair
        ByteArrayOutputStream encryptedOutputStream = new ByteArrayOutputStream();
        EncryptionKeyPair keyPair = SecurePropertiesEncryption.encryptedPropertiesToFile(encryptedOutputStream, originalProperties);
        assertNotNull(keyPair);

        // Decrypt properties using the generated key pair
        byte[] encryptedData = encryptedOutputStream.toByteArray();
        Properties decryptedProperties = SecurePropertiesEncryption.decryptProperties(encryptedData, keyPair);
        assertNotNull(decryptedProperties);

        // Compare original and decrypted properties
        assertEquals(originalProperties.size(), decryptedProperties.size());
        assertEquals(originalProperties.getProperty("key1"), decryptedProperties.getProperty("key1"));
        assertEquals(originalProperties.getProperty("key2"), decryptedProperties.getProperty("key2"));
    }
}
