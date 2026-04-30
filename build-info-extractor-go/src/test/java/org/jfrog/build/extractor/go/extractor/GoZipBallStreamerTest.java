package org.jfrog.build.extractor.go.extractor;

import com.github.zafarkhaja.semver.Version;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.lang.reflect.Field;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


public class GoZipBallStreamerTest {
    @Test(dataProvider = "testIsSubModuleProvider")
    public void testIsSubModule(String subModuleName, String entryName, boolean expectedResult) {
        GoZipBallStreamer goZipBallStreamer = new GoZipBallStreamer(null, "ignore", "ignore", null);
        goZipBallStreamer.setSubModuleNameExplicitly(subModuleName);
        boolean res = goZipBallStreamer.isSubModule(entryName);
        Assert.assertEquals(res, expectedResult);
    }

    @DataProvider
    private Object[][] testIsSubModuleProvider() {
        return new Object[][]{
                {"", "root/go.mod", false},
                {"", "go.mod", false},
                {"", "root/v2/go.mod", true},
                {"v2", "root/go.mod", true},
                {"v2", "root/v2/go.mod", false}
        };
    }

    @Test(dataProvider = "vendorPackageProvider")
    public void testIsVendorPackage(String goModVersion, String entryName, boolean expected) throws Exception {
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "1.0.0", null);
        streamer.goModVersion = Version.parse(goModVersion);

        Method method = GoZipBallStreamer.class.getDeclaredMethod("isVendorPackage", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(streamer, entryName);

        Assert.assertEquals(result, expected, "Failed for: " + entryName + " with Go version: " + goModVersion);
    }

    @DataProvider
    public Object[][] vendorPackageProvider() {
        return new Object[][]{
                // followed by Go tests scenarios:
                //https://cs.opensource.google/go/x/mod/+/c8a731972177c6ce4073699c705e55918ee7be09:zip/vendor_test.go

                // allVers (pre-1.24 and 1.24+)
                {"1.23.0", "vendor/foo/foo.go", true},
                {"1.24.0", "vendor/foo/foo.go", true},
                {"1.23.0", "pkg/vendor/foo/foo.go", true},
                {"1.24.0", "pkg/vendor/foo/foo.go", true},
                {"1.23.0", "longpackagename/vendor/foo/foo.go", true},
                {"1.24.0", "longpackagename/vendor/foo/foo.go", true},
                {"1.23.0", "vendor/vendor.go", false},
                {"1.24.0", "vendor/vendor.go", false},
                {"1.23.0", "vendor/foo/modules.txt", true},
                {"1.24.0", "vendor/foo/modules.txt", true},
                {"1.23.0", "modules.txt", false},
                {"1.24.0", "modules.txt", false},
                {"1.23.0", "vendor/amodules.txt", false},
                {"1.24.0", "vendor/amodules.txt", false},

                // vendor/modules.txt special cases
                {"1.23.0", "vendor/modules.txt", false},
                {"1.24.0", "vendor/modules.txt", true},
                {"1.25.0", "vendor/modules.txt", true},
                {"1.23.0", "foo/vendor/modules.txt", false},
                {"1.24.0", "foo/vendor/modules.txt", true},
                {"1.25.0", "foo/vendor/modules.txt", true},

                // pkg/vendor/vendor.go and longpackagename/vendor/vendor.go
                {"1.23.0", "pkg/vendor/vendor.go", false},
                {"1.23.0", "pkg/vendor/foo/vendor.text", true},
                {"1.24.0", "pkg/vendor/vendor.go", false},
                {"1.23.0", "longpackagename/vendor/vendor.go", false},
                {"1.24.0", "longpackagename/vendor/vendor.go", false},

                {"1.24.0", "src/vendor/github.com/pkg/errors/errors.go", true},
                {"1.23.0", "src/vendor/github.com/pkg/errors/errors.go", true},
                {"1.24.0", "vendor/errors.go", false},
                {"1.24.0", "foo/bar/vendor/pkg/file.go", true},
                {"1.24.0", "notvendor/file.go", false},
                {"1.24.0", "vendor/file.go", false},
                {"1.24.0", "vendor/pkg/file.go", true},
                {"1.24.0", "vendor/", false},
                {"1.24.0", "foo/vendor/pkg/file.go", true},
                {"1.24.0", "foo/vendor/file.go", false}
        };
    }

    @Test
    void testSetSubModuleNameExplicitlySetFlag() throws Exception {
        // Test that setSubModuleName sets the explicitlySet flag
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "v1.0.0", null);

        // Initially, subModuleNameExplicitlySet should be false
        Field explicitlySetField = GoZipBallStreamer.class.getDeclaredField("subModuleNameExplicitlySet");
        explicitlySetField.setAccessible(true);
        Assert.assertFalse((boolean) explicitlySetField.get(streamer), "Initially subModuleNameExplicitlySet should be false");

        // After calling setSubModuleName, it should be true
        streamer.setSubModuleNameExplicitly("submodule");
        Assert.assertTrue((boolean) explicitlySetField.get(streamer),
                "After setSubModuleName, subModuleNameExplicitlySet should be true");

        // Even when setting empty string, flag should be true
        streamer.setSubModuleNameExplicitly("");
        Assert.assertTrue((boolean) explicitlySetField.get(streamer),
                "After setSubModuleName with empty string, subModuleNameExplicitlySet should still be true");
    }

    @Test
    void testInitiateProjectTypeSkipsDetectionWhenExplicitlySet() throws Exception {
        // Test that initiateProjectType skips automatic detection when submodule is explicitly set
        Log log = new NullLog();
        GoZipBallStreamer streamer =
                new GoZipBallStreamer(null, "gitlab.com/group/subgroup/project", "v1.0.0", log);

        // Set submodule name explicitly (simulating GitLabIntelligentFetcher behavior)
        streamer.setSubModuleNameExplicitly(""); // Empty means no submodule

        // Call initiateProjectType via reflection
        Method initiateMethod = GoZipBallStreamer.class.getDeclaredMethod("initiateProjectType");
        initiateMethod.setAccessible(true);
        initiateMethod.invoke(streamer);

        // Verify that subModuleName remains empty (not detected as "subgroup" by automatic detection)
        Field subModuleField = GoZipBallStreamer.class.getDeclaredField("subModuleName");
        subModuleField.setAccessible(true);
        String subModuleName = (String) subModuleField.get(streamer);
        Assert.assertEquals(subModuleName, "", "Submodule name should remain empty when explicitly set");
    }

    @Test
    void testInitiateProjectTypeUsesExplicitlySetSubmodule() throws Exception {
        // Test that explicitly set non-empty submodule is used
        Log log = new NullLog();
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "gitlab.com/group/project", "v1.0.0", log);

        // Set submodule name explicitly
        streamer.setSubModuleNameExplicitly("api");

        // Call initiateProjectType via reflection
        Method initiateMethod = GoZipBallStreamer.class.getDeclaredMethod("initiateProjectType");
        initiateMethod.setAccessible(true);
        initiateMethod.invoke(streamer);

        // Verify that subModuleName remains "api" (not changed by automatic detection)
        Field subModuleField = GoZipBallStreamer.class.getDeclaredField("subModuleName");
        subModuleField.setAccessible(true);
        String subModuleName = (String) subModuleField.get(streamer);
        Assert.assertEquals(subModuleName, "api", "Submodule name should remain 'api' when explicitly set");
    }

    @Test
    void testSetSubModuleNameExplicitlyHandlesNull() {
        // Test that setSubModuleName handles null gracefully
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "v1.0.0", null);

        streamer.setSubModuleNameExplicitly(null);

        // Should not throw exception and should set to empty string
        // We can verify by checking that explicitlySet flag is true
        try {
            Field explicitlySetField = GoZipBallStreamer.class.getDeclaredField("subModuleNameExplicitlySet");
            explicitlySetField.setAccessible(true);
            Assert.assertTrue((boolean) explicitlySetField.get(streamer),
                    "subModuleNameExplicitlySet should be true even when null is passed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tests that scanEntries() correctly identifies the submodule root directory by stripping the first
     * path element (the zip-level prefix) from each directory and comparing it to subModuleName.
     * Only files under the matching submodule directory should appear in the output zip.
     */
    @Test(dataProvider = "subModulePackingProvider")
    public void testScanEntriesFindsSubModuleRootByStrippingZipPrefix(
            Map<String, String> inputEntries,
            String projectName,
            String version,
            Set<String> expectedOutputEntries,
            Set<String> unexpectedOutputEntries) throws IOException {

        File inputZip = createZipWithEntries(inputEntries);
        File outputZip = File.createTempFile("output", ".zip");
        try {
            try (ZipFile zipFile = ZipFile.builder().setFile(inputZip).get()) {
                try (GoZipBallStreamer streamer = new GoZipBallStreamer(zipFile, projectName, version, new NullLog())) {
                    streamer.writeDeployableZip(outputZip);
                }
            }

            Set<String> outputEntryNames = getZipEntryNames(outputZip);

            for (String expected : expectedOutputEntries) {
                Assert.assertTrue(outputEntryNames.contains(expected),
                        "Expected entry missing from output zip: " + expected + ". Actual entries: " + outputEntryNames);
            }
            for (String unexpected : unexpectedOutputEntries) {
                Assert.assertFalse(outputEntryNames.contains(unexpected),
                        "Unexpected entry found in output zip: " + unexpected + ". Actual entries: " + outputEntryNames);
            }
        } finally {
            Files.deleteIfExists(inputZip.toPath());
            Files.deleteIfExists(outputZip.toPath());
        }
    }

    @DataProvider
    private Object[][] subModulePackingProvider() {
        // Scenario 1: Basic submodule - zip prefix is "myrepo-v1.0.0/", submodule is "submod".
        // stripFirstPathElement("myrepo-v1.0.0/submod") == "submod" and finds moduleRootDir correctly.
        Map<String, String> basicSubModuleEntries = new LinkedHashMap<>();
        basicSubModuleEntries.put("myrepo-v1.0.0/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/root.go", "package main\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/submod/go.mod", "module github.com/owner/repo/submod\n\ngo 1.21.0\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/submod/submod.go", "package submod\n");

        // Scenario 2: Submodule with nested directories - non-submodule dirs are excluded.
        Map<String, String> nestedSubModuleEntries = new LinkedHashMap<>();
        nestedSubModuleEntries.put("repo-abc123/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        nestedSubModuleEntries.put("repo-abc123/util/util.go", "package util\n");
        nestedSubModuleEntries.put("repo-abc123/lib/go.mod", "module github.com/owner/repo/lib\n\ngo 1.21.0\n");
        nestedSubModuleEntries.put("repo-abc123/lib/lib.go", "package lib\n");
        nestedSubModuleEntries.put("repo-abc123/lib/helper/helper.go", "package helper\n");

        // Scenario 3: Submodule with root LICENSE included.
        Map<String, String> subModuleWithLicenseEntries = new LinkedHashMap<>();
        subModuleWithLicenseEntries.put("proj-v2.0.0/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/LICENSE", "MIT License\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/main.go", "package main\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/submod/go.mod", "module github.com/owner/repo/submod\n\ngo 1.21.0\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/submod/api.go", "package submod\n");

        // Scenario 4 (RTDEV-82256): Ambiguous submodule name - another directory deeper in the tree
        // has the same trailing name as the real submodule ("hello"). The old endsWith() match would
        // pick "docs/godocs/hello" instead of the actual "hello" submodule root, causing the real
        // module sources to be dropped.
        Map<String, String> ambiguousSubModuleEntries = new LinkedHashMap<>();
        ambiguousSubModuleEntries.put("go-example-abc123/go.mod", "module github.com/owner/go-example\n\ngo 1.21.0\n");
        ambiguousSubModuleEntries.put("go-example-abc123/main.go", "package main\n");
        ambiguousSubModuleEntries.put("go-example-abc123/docs/godocs/hello/README.md", "# Hello docs\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/go.mod", "module github.com/owner/go-example/hello\n\ngo 1.21.0\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/hello.go", "package hello\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/world/world.go", "package world\n");

        return new Object[][]{
                {
                        // Scenario 1: Only submodule files (submod.go, go.mod) included; root.go excluded.
                        basicSubModuleEntries,
                        "github.com/owner/repo/submod",
                        "v1.0.0",
                        new HashSet<>(Arrays.asList(
                                "github.com/owner/repo/submod@v1.0.0/submod.go",
                                "github.com/owner/repo/submod@v1.0.0/go.mod"
                        )),
                        new HashSet<>(Collections.singletonList("github.com/owner/repo/submod@v1.0.0/root.go"))
                },
                {
                        // Scenario 2: lib submodule files included; util/ (outside lib) excluded.
                        nestedSubModuleEntries,
                        "github.com/owner/repo/lib",
                        "v1.0.0",
                        new HashSet<>(Arrays.asList(
                                "github.com/owner/repo/lib@v1.0.0/lib.go",
                                "github.com/owner/repo/lib@v1.0.0/go.mod",
                                "github.com/owner/repo/lib@v1.0.0/helper/helper.go"
                        )),
                        new HashSet<>(Collections.singletonList("github.com/owner/repo/lib@v1.0.0/util.go"))
                },
                {
                        // Scenario 3: Root LICENSE is included in submodule output; main.go is excluded.
                        subModuleWithLicenseEntries,
                        "github.com/owner/repo/submod",
                        "v2.0.0",
                        new HashSet<>(Arrays.asList(
                                "github.com/owner/repo/submod@v2.0.0/api.go",
                                "github.com/owner/repo/submod@v2.0.0/go.mod",
                                "github.com/owner/repo/submod@v2.0.0/LICENSE"
                        )),
                        new HashSet<>(Collections.singletonList("github.com/owner/repo/submod@v2.0.0/main.go"))
                },
                {
                        // Scenario 4 (RTDEV-82256): Ambiguous trailing name - "docs/godocs/hello" must NOT
                        // be picked as module root; only the real "hello" submodule files should appear.
                        ambiguousSubModuleEntries,
                        "github.com/owner/go-example/hello",
                        "v1.0.0",
                        new HashSet<>(Arrays.asList(
                                "github.com/owner/go-example/hello@v1.0.0/hello.go",
                                "github.com/owner/go-example/hello@v1.0.0/go.mod",
                                "github.com/owner/go-example/hello@v1.0.0/world/world.go"
                        )),
                        new HashSet<>(Arrays.asList(
                                "github.com/owner/go-example/hello@v1.0.0/main.go",
                                "github.com/owner/go-example/hello@v1.0.0/README.md",
                                "github.com/owner/go-example/hello@v1.0.0/docs/godocs/hello/README.md"
                        ))
                }
        };
    }

    // ── Helper methods ──────────────────────────────────────────────────────────

    /**
     * Creates a temporary zip file populated with the given entries (name → content).
     */
    private File createZipWithEntries(Map<String, String> entries) throws IOException {
        File tempZip = File.createTempFile("test-input", ".zip");
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(tempZip)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                byte[] content = e.getValue().getBytes(StandardCharsets.UTF_8);
                ZipArchiveEntry ze = new ZipArchiveEntry(e.getKey());
                ze.setSize(content.length);
                zos.putArchiveEntry(ze);
                zos.write(content);
                zos.closeArchiveEntry();
            }
        }
        return tempZip;
    }

    /**
     * Returns the set of all entry names contained in the given zip file.
     */
    private Set<String> getZipEntryNames(File zip) throws IOException {
        try (ZipFile zipFile = ZipFile.builder().setFile(zip).get()) {
            Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
            Set<String> names = new HashSet<>();
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().getName());
            }
            return names;
        }
    }

}