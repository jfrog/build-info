package org.jfrog.build.extractor.go.extractor;


import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class GoVersionUtilsTest {

    @Test
    public void getMajorVersion_ValidVersion_ReturnsMajorVersion() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorVersion("v0.0.4", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v2.2.3", log), 2);
        assertEquals(GoVersionUtils.getMajorVersion("v10.20.30", log), 10);
        assertEquals(GoVersionUtils.getMajorVersion("v1.1.2-prerelease+meta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.1.2+meta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha.beta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha.1", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha.0valid", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-rc.1+build.1", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3-beta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v10.2.3-DEV-SNAPSHOT", log), 10);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3-SNAPSHOT-123", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v2.0.0+build.1848", log), 2);
        assertEquals(GoVersionUtils.getMajorVersion("v2.0.1-alpha.1227", log), 2);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha+beta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3----RC-SNAPSHOT.12.9.1--.12+788", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3----R-S.12.9.1--.12+meta", log), 1);
        assertEquals(GoVersionUtils.getMajorVersion("v2.2.0-beta.1", log), 2);
        assertEquals(GoVersionUtils.getMajorVersion("v2.0.0-beta-1", log), 2);
    }

    @Test
    public void getMajorVersion_InvalidVersion_ReturnsZero() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorVersion("invalid.version", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion(null, log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v01.1.1", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v9.8.7-whatever+meta+meta", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3.DEV", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.3-0123", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.0.0-alpha_beta", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2-SNAPSHOT", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("v1.2.31.2.3----RC-SNAPSHOT.12.09.1--..12+788", log), 0);
    }

    @Test
    public void getMajorProjectVersion_ValidProject_ReturnsMajorVersion() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorProjectVersion("github.com/owner/repo/v3", log), 3);
        assertEquals(GoVersionUtils.getMajorProjectVersion("github.com/owner/repo/v2", log), 2);
    }

    @Test
    public void getMajorProjectVersion_InvalidProject_ReturnsZeroOrOne() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorProjectVersion("github.com/owner/repo", log), 0);
        assertEquals(GoVersionUtils.getMajorProjectVersion("", log), 0);
        assertEquals(GoVersionUtils.getMajorProjectVersion(null, log), 0);
    }

    @Test
    public void getCleanVersion_WithIncompatible_ReturnsCleanVersion() {
        assertEquals(GoVersionUtils.getCleanVersion("1.2.3+incompatible"), "1.2.3");
    }

    @Test
    public void getCleanVersion_WithoutIncompatible_ReturnsSameVersion() {
        assertEquals(GoVersionUtils.getCleanVersion("1.2.3"), "1.2.3");
    }

    @Test
    public void isCompatibleGoModuleNaming_ValidInputs_ReturnsTrue() {
        Log log = new NullLog();
        assertTrue(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo/v2", "v2.0.5", log));
        assertTrue(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo", "v1.0.5", log));
    }

    @Test
    public void isCompatibleGoModuleNaming_InvalidInputs_ReturnsFalse() {
        Log log = new NullLog();
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo", "v2.0.5", log));
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo/v2", "v2.0.5+incompatible", log));
    }

    @Test
    public void getSubModule_ValidProjectName_ReturnsSubModule() {
        assertEquals(GoVersionUtils.getSubModule("github.com/owner/repo/submodule"), "submodule");
    }

    @Test
    public void getSubModule_InvalidProjectName_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getSubModule("github.com/owner/repo"), "");
        assertEquals(GoVersionUtils.getSubModule(""), "");
        assertEquals(GoVersionUtils.getSubModule(null), "");
    }

    @Test
    public void getParent_ValidPath_ReturnsParentPath() {
        assertEquals(GoVersionUtils.getParent("/path/to/file"), "/path/to");
    }

    @Test
    public void getParent_InvalidPath_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getParent(""), "");
        assertEquals(GoVersionUtils.getParent(null), "");
    }

    @Test
    public void getMajorVersion() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorVersion("", log), 0);
        assertEquals(GoVersionUtils.getMajorVersion(null, log), 0);
        assertEquals(GoVersionUtils.getMajorVersion("vX.1.2", log), 0);
    }

    @Test
    public void getMajorProjectVersion_InvalidMajorVersion_LogsErrorAndReturnsZero() {
        Log log = new NullLog();
        assertEquals(GoVersionUtils.getMajorProjectVersion("github.com/owner/repo/vX", log), 0);
    }

    @Test
    public void getCleanVersion_NullVersion_ReturnsNull() {
        assertNull(GoVersionUtils.getCleanVersion(null));
    }

    @Test
    public void isCompatibleGoModuleNaming_EmptyProjectName_ReturnsFalse() {
        Log log = new NullLog();
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming("", "v2.0.5", log));
    }

    @Test
    public void isCompatibleGoModuleNaming_EmptyVersion_ReturnsFalse() {
        Log log = new NullLog();
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo/v2", "", log));
    }

    @Test
    public void isCompatibleGoModuleNaming_NullProjectName_ReturnsFalse() {
        Log log = new NullLog();
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming(null, "v2.0.5", log));
    }

    @Test
    public void isCompatibleGoModuleNaming_NullVersion_ReturnsFalse() {
        Log log = new NullLog();
        assertFalse(GoVersionUtils.isCompatibleGoModuleNaming("github.com/owner/repo/v2", null, log));
    }

    @Test
    public void getSubModule_EmptyProjectName_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getSubModule(""), "");
    }

    @Test
    public void getSubModule_NullProjectName_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getSubModule(null), "");
    }

    @Test
    public void getParent_EmptyPath_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getParent(""), "");
    }

    @Test
    public void getParent_NullPath_ReturnsEmptyString() {
        assertEquals(GoVersionUtils.getParent(null), "");
    }

}