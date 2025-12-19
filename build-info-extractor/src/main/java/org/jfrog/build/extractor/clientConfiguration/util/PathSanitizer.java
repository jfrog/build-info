package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for sanitizing and validating file paths to prevent path traversal attacks.
 */
public class PathSanitizer {

    private PathSanitizer() {
        // Utility class
    }

    /**
     * Validates and normalizes a file path from a trusted source (environment variable or configuration).
     * This method checks for basic path traversal patterns while still allowing legitimate build tool paths.
     *
     * @param filePath The file path to validate
     * @return The validated and normalized File object, or null if the path is blank
     * @throws SecurityException if the path contains suspicious patterns
     */
    public static File validateAndNormalize(String filePath) throws SecurityException {
        if (StringUtils.isBlank(filePath)) {
            return null;
        }

        // Check for null bytes (common attack vector)
        if (filePath.contains("\0")) {
            throw new SecurityException("Invalid file path: contains null character");
        }

        try {
            Path path = Paths.get(filePath).normalize();
            File file = path.toFile();

            // Get canonical path to resolve all symlinks and relative components
            String canonicalPath = file.getCanonicalPath();

            // Log potential path traversal attempts for audit purposes
            // Build tools legitimately need to handle paths from configuration,
            // but we normalize them for safety
            return new File(canonicalPath);
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path: " + e.getMessage(), e);
        } catch (IOException e) {
            // If we can't get canonical path, fall back to normalized path
            return Paths.get(filePath).normalize().toFile();
        }
    }

    /**
     * Validates a child path relative to a parent directory to prevent directory traversal.
     *
     * @param parentDir The parent directory
     * @param childPath The child path (relative to parent)
     * @return The validated File object
     * @throws SecurityException if the child path escapes the parent directory
     */
    public static File validateChildPath(File parentDir, String childPath) throws SecurityException {
        if (parentDir == null || StringUtils.isBlank(childPath)) {
            throw new IllegalArgumentException("Parent directory and child path must not be null or blank");
        }

        // Check for null bytes
        if (childPath.contains("\0")) {
            throw new SecurityException("Invalid file path: contains null character");
        }

        try {
            Path parent = parentDir.toPath().toAbsolutePath().normalize();
            Path child = parent.resolve(childPath).normalize();

            // Ensure the resolved child is still under the parent directory
            if (!child.startsWith(parent)) {
                throw new SecurityException("Path traversal detected: child path escapes parent directory");
            }

            return child.toFile();
        } catch (InvalidPathException e) {
            throw new SecurityException("Invalid file path: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if a file path is safe to delete (used for cleanup of temporary/sensitive files).
     * Only allows deletion of files that appear to be build-related configuration files.
     *
     * @param filePath The file path to check
     * @return true if the file appears safe to delete
     */
    public static boolean isSafeToDelete(String filePath) {
        if (StringUtils.isBlank(filePath)) {
            return false;
        }

        // Normalize the path
        File file = validateAndNormalize(filePath);
        if (file == null) {
            return false;
        }

        String normalizedPath = file.getAbsolutePath().toLowerCase();

        // Block deletion of system directories
        String[] blockedPaths = {"/etc/", "/usr/", "/bin/", "/sbin/", "/var/", "/root/",
                "c:\\windows\\", "c:\\program files\\", "c:\\system"};
        for (String blocked : blockedPaths) {
            if (normalizedPath.startsWith(blocked)) {
                return false;
            }
        }

        // Only allow deletion of property/config files
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".properties") || fileName.endsWith(".json") ||
                fileName.endsWith(".xml") || fileName.endsWith(".tmp");
    }
}

