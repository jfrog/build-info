package org.jfrog.build.extractor.util.encryption;

import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.PropertyEncryptor;
import org.testng.annotations.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class PropertyEncryptorTest {

    @Test
    public void testDecryptPropertiesFromFile() throws IOException, InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        // Create a temporary file with encrypted properties
        Path tempFile = Files.createTempFile("encrypted_properties", ".properties");
        File file = tempFile.toFile();

        Properties properties = new Properties();
        properties.setProperty("key1", "value1");
        properties.setProperty("key2", "value2");
        EncryptionKeyPair keyPair;
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            // Encrypt properties and write to the file
            keyPair = PropertyEncryptor.encryptedPropertiesToStream(fileOutputStream, properties);
        }

        String encryptedFilePath = file.getAbsolutePath();

        try {
            // Decrypt properties from the encrypted file
            Properties decryptedProperties = PropertyEncryptor.decryptPropertiesFromFile(encryptedFilePath, keyPair);

            // Check if decrypted properties are as expected
            assertNotNull(decryptedProperties);
            assertEquals("value1", decryptedProperties.getProperty("key1"), "Decrypted property 'key1' should match");
            assertEquals("value2", decryptedProperties.getProperty("key2"), "Decrypted property 'key2' should match");
        } finally {
            // Clean up - delete the temporary file
            Files.deleteIfExists(Paths.get(encryptedFilePath));
        }
    }
}
