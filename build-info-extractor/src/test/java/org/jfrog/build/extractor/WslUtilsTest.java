package org.jfrog.build.extractor;

import org.apache.commons.lang3.SystemUtils;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class WslUtilsTest {

    @DataProvider
    public Object[][] wslPathDetection() {
        return new Object[][]{
                {null, false},
                {"", false},
                {"C:\\dev\\project", false},
                {"\\\\wsl$\\Ubuntu\\home\\user\\repo", true},
                {"\\\\WSL$\\Ubuntu\\home\\user\\repo", true},
                {"\\\\wsl.localhost\\Ubuntu\\home\\user\\repo", true},
                {"\\\\WSL.LOCALHOST\\Ubuntu\\home\\user\\repo", true},
                {"\\\\?\\UNC\\wsl$\\Ubuntu\\home\\user\\repo", true},
                {"\\\\?\\UNC\\WSL.LOCALHOST\\Ubuntu\\home\\user\\repo", true},
        };
    }

    @Test(dataProvider = "wslPathDetection")
    public void testIsWslPathString(String path, boolean expected) {
        assertEquals(WslUtils.isWslPath(path), expected);
    }

    @Test(dataProvider = "wslPathDetection")
    public void testIsWslPathPathObject(String pathString, boolean expected) {
        if (pathString == null) {
            assertFalse(WslUtils.isWslPath((java.nio.file.Path) null));
        } else {
            assertEquals(WslUtils.isWslPath(Paths.get(pathString)), expected);
        }
    }

    @DataProvider
    public Object[][] toLinuxPathCases() {
        return new Object[][]{
                {null, null},
                {"", ""},
                {"C:\\dev", "C:\\dev"},
                {"\\\\wsl$\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\WSL$\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\wsl.localhost\\Ubuntu\\home\\user\\app", "/home/user/app"},
                {"\\\\wsl$\\Ubuntu", "/"},
                {"\\\\?\\UNC\\wsl$\\Ubuntu\\home\\user\\app", "/home/user/app"},
        };
    }

    @Test(dataProvider = "toLinuxPathCases")
    public void testToLinuxPath(String input, String expected) {
        assertEquals(WslUtils.toLinuxPath(input), expected);
    }

    @DataProvider
    public Object[][] windowsToMountCases() {
        return new Object[][]{
                {"C:\\Users\\u\\tmp", "/mnt/c/Users/u/tmp"},
                {"C:/Users/u/tmp", "/mnt/c/Users/u/tmp"},
                {"d:\\proj", "/mnt/d/proj"},
                {"\\\\wsl$\\Ubuntu\\home\\u\\p", "/home/u/p"},
                {"\\\\?\\C:\\Users\\u\\tmp", "/mnt/c/Users/u/tmp"},
                {"\\\\some-server\\share\\a", "//some-server/share/a"},
        };
    }

    @Test(dataProvider = "windowsToMountCases")
    public void testWindowsLocalPathToWslMount(String input, String expected) {
        assertEquals(WslUtils.windowsLocalPathToWslMount(input), expected);
    }

    @DataProvider
    public Object[][] wslDistributionCases() {
        return new Object[][]{
                {null, null},
                {"C:\\dev\\project", null},
                {"\\\\wsl$\\Ubuntu\\home\\user\\repo", "Ubuntu"},
                {"\\\\WSL$\\Ubuntu\\home\\user\\repo", "Ubuntu"},
                {"\\\\wsl.localhost\\Debian\\home\\user\\repo", "Debian"},
                {"\\\\WSL.LOCALHOST\\MyDistro\\home\\user\\repo", "MyDistro"},
                {"\\\\?\\UNC\\wsl$\\Ubuntu\\home\\user\\repo", "Ubuntu"},
        };
    }

    @Test(dataProvider = "wslDistributionCases")
    public void testGetWslDistribution(String path, String expected) {
        assertEquals(WslUtils.getWslDistribution(path), expected);
    }

    @DataProvider
    public Object[][] linuxToWslWindowsCases() {
        return new Object[][]{
                {"/home/user/project", "Ubuntu", "\\\\wsl.localhost\\Ubuntu\\home\\user\\project"},
                {"/home/user/project/build/gradle-dep-tree/abc", "Debian",
                        "\\\\wsl.localhost\\Debian\\home\\user\\project\\build\\gradle-dep-tree\\abc"},
        };
    }

    @Test(dataProvider = "linuxToWslWindowsCases")
    public void testLinuxPathToWslWindowsPath(String linuxPath, String distro, String expected) {
        assertEquals(WslUtils.linuxPathToWslWindowsPath(linuxPath, distro), expected);
    }

    @DataProvider
    public Object[][] normalizePathStringForWslCases() {
        return new Object[][]{
                {null, null},
                {"", ""},
                {"\\\\?\\UNC\\wsl$\\Ubuntu\\home\\u", "\\\\wsl$\\Ubuntu\\home\\u"},
                {"\\\\?\\C:\\Users\\x", "C:\\Users\\x"},
                {"C:\\plain\\path", "C:\\plain\\path"},
        };
    }

    @Test(dataProvider = "normalizePathStringForWslCases")
    public void testNormalizePathStringForWsl(String input, String expected) {
        assertEquals(WslUtils.normalizePathStringForWsl(input), expected);
    }

    @Test
    public void testToWslLinuxCdPathNull() {
        assertEquals(WslUtils.toWslLinuxCdPath(null), "/");
    }

    @Test
    public void testToWslLinuxCdPathNonWindows() {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw new SkipException("Uses unix absolute File paths");
        }
        File dir = Paths.get("/tmp", "wsl-cd-test").toFile();
        String expected = dir.getAbsolutePath().replace('\\', '/');
        assertEquals(WslUtils.toWslLinuxCdPath(dir), expected);
    }

    @Test
    public void testToWslLinuxCdPathWslUnc() {
        if (!SystemUtils.IS_OS_WINDOWS) {
            throw new SkipException("UNC paths as File are only meaningful on Windows");
        }
        String dir = "\\\\wsl$\\Ubuntu\\home\\user\\repo";
        assertEquals(WslUtils.toWslLinuxCdPath(new File(dir)), "/home/user/repo");
    }
}
