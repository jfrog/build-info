package org.jfrog.build.extractor.util.encryption;

import org.jfrog.build.extractor.clientConfiguration.util.encryption.EncryptionKeyPair;
import org.jfrog.build.extractor.clientConfiguration.util.encryption.Encryptor;
import org.testng.annotations.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static org.testng.AssertJUnit.assertEquals;

public class EncryptorTest {
    @Test
    public void testEncryptionDecryption() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        EncryptionKeyPair keyPair = new EncryptionKeyPair();
        // Data to be encrypted
        String originalData = "This is a secret message";
        byte[] dataToEncrypt = originalData.getBytes();

        // Encrypt the data
        byte[] encryptedData = Encryptor.encrypt(dataToEncrypt, keyPair);

        // Decrypt the data
        byte[] decryptedData = Encryptor.decrypt(encryptedData, keyPair);

        // Verify if decrypted data matches the original data
        String decryptedMessage = new String(decryptedData);
        assertEquals(originalData, decryptedMessage);
    }
}
