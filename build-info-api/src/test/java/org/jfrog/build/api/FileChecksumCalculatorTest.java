/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.api;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Tests the behavior of the checksum calculator
 *
 * @author Noam Y. Tenne
 */
@Test
public class FileChecksumCalculatorTest {

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
            expectedExceptionsMessageRegExp = "Cannot read checksums of non-existent file.")
    public void testNonExistingFile() throws IOException, NoSuchAlgorithmException {
        FileChecksumCalculator.calculateChecksums(new File("/this/file/doesnt/exists.moo"));
    }

    /**
     * Tests the behavior of the calculator when given a folder
     */
    @Test(expectedExceptions = IllegalArgumentException.class,
            expectedExceptionsMessageRegExp = "Cannot read checksums of a folder.")
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
        BufferedWriter out = new BufferedWriter(new FileWriter(tempFile));
        out.write("This is a test file");
        out.close();
        Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(tempFile, "md5", "sha1");
        String md5 = getChecksum("md5", tempFile);
        String sha1 = getChecksum("sha1", tempFile);
        assertEquals(checksumsMap.get("md5"), md5, "Unexpected test file MD5 checksum value.");
        assertEquals(checksumsMap.get("sha1"), sha1, "Unexpected test file SHA1 checksum value.");
    }

    /**
     * Returns the checksum of the given file
     *
     * @param algorithm  Algorithm to calculate by
     * @param fileToRead File to calculate
     * @return Checksum value
     * @throws NoSuchAlgorithmException Thrown if MD5 or SHA1 aren't supported
     * @throws IOException              Thrown if any error occurs while reading the file or calculating the checksum
     */
    private String getChecksum(String algorithm, File fileToRead) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        FileInputStream inputStream = new FileInputStream(fileToRead);

        byte[] buffer = new byte[32768];
        try {
            int size = inputStream.read(buffer, 0, 32768);

            while (size >= 0) {
                digest.update(buffer, 0, size);
                size = inputStream.read(buffer, 0, 32768);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        byte[] bytes = digest.digest();
        if (bytes.length != 16 && bytes.length != 20) {
            int bitLength = bytes.length * 8;
            throw new IllegalArgumentException("Unrecognised length for binary data: " + bitLength + " bits");
        }
        StringBuilder sb = new StringBuilder();
        for (byte aBinaryData : bytes) {
            String t = Integer.toHexString(aBinaryData & 0xff);
            if (t.length() == 1) {
                sb.append("0");
            }
            sb.append(t);
        }
        return sb.toString().trim();
    }
}