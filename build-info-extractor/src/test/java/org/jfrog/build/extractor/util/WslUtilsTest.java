package org.jfrog.build.extractor.util;

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
        };
    }

    @Test(dataProvider = "windowsToMountCases")
    public void testWindowsLocalPathToWslMount(String input, String expected) {
        assertEquals(WslUtils.windowsLocalPathToWslMount(input), expected);
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
