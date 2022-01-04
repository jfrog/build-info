package org.jfrog.build.api;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.jfrog.build.api.util.FileChecksumCalculator.*;
import static org.testng.Assert.assertEquals;

/**
 * Tests the behavior of the checksum calculator
 *
 * @author Noam Y. Tenne
 */
@Test
public class FileChecksumCalculatorTest {
    private static final String expectedSha256 = "3b538cc62b700a0af5cb4b80515d511bc9ee67efe48d124005d36a7d37ad5b4b";
    private static final String expectedSha1 = "a27d18807577048b59908c4accf425f4eb406220";
    private static final String expectedMd5 = "0a3a30871aa677d0670d4a14125acf24";

    /**
     * Tests the behavior of the checksum calculator when given a null file
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot read checksums of null file.")
    public void testNullFile() throws IOException, NoSuchAlgorithmException {
        FileChecksumCalculator.calculateChecksums(null);
    }

    /**
     * Tests the behavior of the calculator when given a non-existing file
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot read checksums of non-existent file: (.+)")
    public void testNonExistingFile() throws IOException, NoSuchAlgorithmException {
        FileChecksumCalculator.calculateChecksums(new File("/this/file/doesnt/exists.moo"));
    }

    /**
     * Tests the behavior of the calculator when given a folder
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot read checksums of a folder: (.+)")
    public void testFolder() throws IOException, NoSuchAlgorithmException {
        FileChecksumCalculator.calculateChecksums(FileUtils.getTempDirectory());
    }

    /**
     * Tests the behavior of the calculator when given a null algorithms var args
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Checksum algorithms cannot be null.")
    public void testNullAlgorithms() throws IOException, NoSuchAlgorithmException {
        FileChecksumCalculator.calculateChecksums(File.createTempFile("moo", "bla"), (String[]) null);
    }

    /**
     * Tests the behavior of the calculator when given an empty algorithms var args
     */
    public void testEmptyAlgorithms() throws IOException, NoSuchAlgorithmException {
        Map<String, String> checksumsMap =
                FileChecksumCalculator.calculateChecksums(File.createTempFile("moo", "bla"));
        Assert.assertTrue(checksumsMap.isEmpty(), "No algorithms were given to the calculator, checksum value map " +
                "should be empty");
    }

    /**
     * Tests the behavior of the calculator when given a valid file
     */
    public void testValidFile() throws IOException, NoSuchAlgorithmException {
        File tempFile = File.createTempFile("moo", "test");
        try (BufferedWriter out = new BufferedWriter(new FileWriter(tempFile))) {
            out.write("API/FileChecksumCalculatorTest - This is a test file");
        }
        Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(tempFile, MD5_ALGORITHM, SHA1_ALGORITHM, SHA256_ALGORITHM);
        assertEquals(checksumsMap.get(MD5_ALGORITHM), expectedMd5, "Unexpected test file MD5 checksum value.");
        assertEquals(checksumsMap.get(SHA1_ALGORITHM), expectedSha1, "Unexpected test file SHA1 checksum value.");
        assertEquals(checksumsMap.get(SHA256_ALGORITHM), expectedSha256, "Unexpected test file SHA1 checksum value.");
    }
}