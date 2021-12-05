package org.jfrog.build.api.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * File checksum calculator class
 *
 * @author Noam Y. Tenne
 */
public abstract class FileChecksumCalculator {

    private static final int BUFFER_SIZE = 32768;

    /**
     * Calculates the given file's checksums
     *
     * @param fileToCalculate File to calculate
     * @param algorithms      Algorithms to use for calculation
     * @return Map with algorithm keys and checksum values
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     * @throws IOException              Thrown if any error occurs while reading the file or calculating the checksums
     * @throws IllegalArgumentException TThrown if the given file to calc is null or non-existing or the algorithms var
     *                                  args is null
     */
    public static Map<String, String> calculateChecksums(File fileToCalculate, String... algorithms)
            throws NoSuchAlgorithmException, IOException {

        if (fileToCalculate == null) {
            throw new IllegalArgumentException("Cannot read checksums of null file.");
        }

        if (!fileToCalculate.exists()) {
            throw new IllegalArgumentException("Cannot read checksums of non-existent file: "
                    + fileToCalculate.getAbsolutePath());
        }

        if (!fileToCalculate.isFile()) {
            throw new IllegalArgumentException("Cannot read checksums of a folder: " + fileToCalculate.getAbsolutePath());
        }

        if (algorithms == null) {
            throw new IllegalArgumentException("Checksum algorithms cannot be null.");
        }

        if (algorithms.length == 0) {
            return new HashMap<>();
        }

        return calculate(fileToCalculate, algorithms);
    }

    /**
     * Calculates the given file's checksums
     *
     * @param fileToCalculate File to calculate
     * @param algorithms      Algorithms to use for calculation
     * @return Map with algorithm keys and checksum values
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     * @throws IOException              Thrown if any error occurs while reading the file or calculating the checksums
     * @throws IllegalArgumentException Thrown if the given file to calc is null or non-existing or the algorithms var
     *                                  args is null
     */
    private static Map<String, String> calculate(File fileToCalculate, String... algorithms)
            throws NoSuchAlgorithmException, IOException {
        Map<String, MessageDigest> digestMap = new HashMap<>();
        Map<String, String> checksumMap = new HashMap<>();

        for (String algorithm : algorithms) {
            digestMap.put(algorithm, MessageDigest.getInstance(algorithm));
        }

        FileInputStream inputStream = new FileInputStream(fileToCalculate);

        byte[] buffer = new byte[BUFFER_SIZE];
        try {
            int size = inputStream.read(buffer, 0, BUFFER_SIZE);

            while (size >= 0) {
                for (String algorithm : algorithms) {
                    digestMap.get(algorithm).update(buffer, 0, size);
                }
                size = inputStream.read(buffer, 0, BUFFER_SIZE);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        for (String algorithm : algorithms) {
            byte[] bytes = digestMap.get(algorithm).digest();
            StringBuilder sb = new StringBuilder();
            for (byte aBinaryData : bytes) {
                String t = Integer.toHexString(aBinaryData & 0xff);
                if (t.length() == 1) {
                    sb.append("0");
                }
                sb.append(t);
            }
            checksumMap.put(algorithm, sb.toString().trim());
        }

        return checksumMap;
    }
}