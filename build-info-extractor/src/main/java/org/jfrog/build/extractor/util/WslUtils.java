package org.jfrog.build.extractor.util;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.nio.file.Path;

/**
 * WSL (Windows Subsystem for Linux) path handling.
 * On Windows, WSL filesystems are exposed as UNC paths under {@code \\wsl.localhost\} or {@code \\wsl$\}.
 */
public final class WslUtils {

    private static final String WSL_LOCALHOST_PREFIX = "\\\\wsl.localhost\\";
    private static final String WSL_DOLLAR_PREFIX = "\\\\wsl$\\";

    private WslUtils() {
    }

    private static boolean startsWithIgnoreCase(String s, String prefix) {
        return s.length() >= prefix.length() && s.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    /**
     * Normalizes Windows extended-length prefixes so WSL detection sees {@code \\wsl$\...} / {@code \\wsl.localhost\...}.
     * Example: {@code \\?\UNC\wsl$\Ubuntu\home\...} becomes {@code \\wsl$\Ubuntu\home\...}.
     */
    public static String normalizePathStringForWsl(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }
        String p = path;
        if (startsWithIgnoreCase(p, "\\\\?\\UNC\\")) {
            return "\\\\" + p.substring("\\\\?\\UNC\\".length());
        }
        if (startsWithIgnoreCase(p, "\\\\?\\")) {
            return p.substring("\\\\?\\".length());
        }
        return p;
    }

    /**
     * @return true if the path is a UNC path rooted at {@code wsl.localhost} or {@code wsl$}.
     */
    public static boolean isWslPath(String path) {
        if (path == null) {
            return false;
        }
        String normalized = normalizePathStringForWsl(path);
        return startsWithIgnoreCase(normalized, WSL_LOCALHOST_PREFIX) || startsWithIgnoreCase(normalized, WSL_DOLLAR_PREFIX);
    }

    public static boolean isWslPath(Path path) {
        return path != null && isWslPath(path.toString());
    }

    /**
     * Converts a Windows-style WSL UNC path to the Linux path inside WSL (distro segment stripped).
     *
     * @return Linux path, or the original string if not a WSL UNC path
     */
    public static String toLinuxPath(String wslWindowsPath) {
        if (wslWindowsPath == null) {
            return null;
        }
        String p = normalizePathStringForWsl(wslWindowsPath);
        String withoutPrefix;
        if (startsWithIgnoreCase(p, WSL_LOCALHOST_PREFIX)) {
            withoutPrefix = p.substring(WSL_LOCALHOST_PREFIX.length());
        } else if (startsWithIgnoreCase(p, WSL_DOLLAR_PREFIX)) {
            withoutPrefix = p.substring(WSL_DOLLAR_PREFIX.length());
        } else {
            return wslWindowsPath;
        }
        int firstBackslash = withoutPrefix.indexOf('\\');
        if (firstBackslash == -1) {
            return "/";
        }
        return withoutPrefix.substring(firstBackslash).replace('\\', '/');
    }

    /**
     * Maps a Windows drive path (e.g. {@code C:\Users\...\Temp\x}) to the default WSL mount
     * (e.g. {@code /mnt/c/Users/.../Temp/x}). Non-WSL UNC paths and non-Windows paths are returned
     * with backslashes replaced by forward slashes where applicable.
     */
    public static String windowsLocalPathToWslMount(String windowsPath) {
        if (windowsPath == null) {
            return null;
        }
        String p = normalizePathStringForWsl(windowsPath);
        if (isWslPath(p)) {
            return toLinuxPath(p);
        }
        if (p.length() >= 2 && Character.isLetter(p.charAt(0)) && p.charAt(1) == ':') {
            char drive = Character.toLowerCase(p.charAt(0));
            String rest = p.substring(2).replace('\\', '/');
            if (rest.isEmpty()) {
                return "/mnt/" + drive + "/";
            }
            if (rest.charAt(0) != '/') {
                rest = "/" + rest;
            }
            return "/mnt/" + drive + rest;
        }
        return p.replace('\\', '/');
    }

    /**
     * Extracts the WSL distribution name from a WSL UNC path.
     * Example: {@code "\\wsl.localhost\Ubuntu\home\\user"} → {@code "Ubuntu"}.
     *
     * @return the distribution name, or {@code null} if the path is not a WSL UNC path.
     */
    public static String getWslDistribution(String wslPath) {
        if (!isWslPath(wslPath)) {
            return null;
        }
        String normalized = normalizePathStringForWsl(wslPath);
        String withoutPrefix = startsWithIgnoreCase(normalized, WSL_LOCALHOST_PREFIX)
                ? normalized.substring(WSL_LOCALHOST_PREFIX.length())
                : normalized.substring(WSL_DOLLAR_PREFIX.length());
        int sep = withoutPrefix.indexOf('\\');
        return sep == -1 ? withoutPrefix : withoutPrefix.substring(0, sep);
    }

    /**
     * Converts a Linux absolute path back to a Windows WSL UNC path using a known distribution.
     * Example: {@code "/home/user/project"} + {@code "Ubuntu"} → {@code \\wsl.localhost\\Ubuntu\\home\\user\\project}.
     */
    public static String linuxPathToWslWindowsPath(String linuxPath, String distro) {
        return WSL_LOCALHOST_PREFIX + distro + linuxPath.replace('/', '\\');
    }

    /**
     * Linux path suitable for {@code wsl.exe --cd} for the given working directory.
     * WSL UNC → {@link #toLinuxPath}; Windows drive path → {@link #windowsLocalPathToWslMount}; otherwise forward slashes.
     */
    public static String toWslLinuxCdPath(File workingDirectory) {
        if (workingDirectory == null) {
            return "/";
        }
        String path = workingDirectory.getAbsolutePath();
        if (isWslPath(path)) {
            return toLinuxPath(path);
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            return windowsLocalPathToWslMount(path);
        }
        return path.replace('\\', '/');
    }
}
