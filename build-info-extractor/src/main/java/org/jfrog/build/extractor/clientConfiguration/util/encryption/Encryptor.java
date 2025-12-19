package org.jfrog.build.extractor.clientConfiguration.util.encryption;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Provides AES-GCM encryption/decryption functionality.
 * Note: This class uses AES/GCM/NoPadding which is a secure authenticated encryption mode.
 * SAST tools may incorrectly flag this as "RSA with inadequate padding" - this is a false positive
 * as GCM mode provides both encryption and authentication without needing additional padding.
 */
public class Encryptor {
    private static final String ALGORITHM = "AES";
    // AES/GCM/NoPadding is a secure authenticated encryption mode - "NoPadding" is correct for GCM
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;

    /**
     * Decrypts the given data using the provided EncryptionKeyPair.
     *
     * @param data    The encrypted data to be decrypted
     * @param keyPair The EncryptionKeyPair containing the secret key and IV for decryption
     * @return The decrypted data as a byte array
     */
    public static byte[] decrypt(byte[] data, EncryptionKeyPair keyPair) throws IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyPair.getSecretKey(), ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, keyPair.getIv());
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmParameterSpec);
        return cipher.doFinal(data);
    }

    /**
     * Encrypts the given data using the provided EncryptionKeyPair.
     *
     * @param data    The data to be encrypted
     * @param keyPair The EncryptionKeyPair containing the secret key and IV for encryption
     * @return The encrypted data as a byte array
     */
    public static byte[] encrypt(byte[] data, EncryptionKeyPair keyPair) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyPair.getSecretKey(), ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, keyPair.getIv());
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmParameterSpec);
        return cipher.doFinal(data);
    }
}
