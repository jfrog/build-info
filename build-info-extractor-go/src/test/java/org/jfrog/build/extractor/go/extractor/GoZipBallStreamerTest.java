package org.jfrog.build.extractor.go.extractor;

import com.github.zafarkhaja.semver.Version;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class GoZipBallStreamerTest {

    private static final Log log = new NullLog();

    // --- isSubModule unit tests ---

    @Test(dataProvider = "isSubModuleProvider")
    public void testIsSubModule(String subModuleName, String entryName, boolean expectedResult) {
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "ignore", "ignore", null);
        streamer.setSubModuleNameExplicitly(subModuleName);
        assertEquals(streamer.isSubModule(entryName), expectedResult,
                "subModule='" + subModuleName + "', entry='" + entryName + "'");
    }

    @DataProvider
    private Object[][] isSubModuleProvider() {
        return new Object[][]{
                {"", "root/go.mod", false},
                {"", "go.mod", false},
                {"", "root/v2/go.mod", true},
                {"v2", "root/go.mod", true},
                {"v2", "root/v2/go.mod", false},
                {"pkg/module1", "root/pkg/module1/go.mod", false},
                {"pkg/module1", "root/pkg/module2/go.mod", true},
                {"pkg/module1", "root/go.mod", true},
                {"some/submodule", "root/some/submodule/go.mod", false},
                {"some/submodule", "root/some/submodule/should ignore/go.mod", true},
                {"pkg/module1", "root/pkg/module1/handler.go", false},
                {"submodule/v2", "root/submodule/v2/go.mod", false},
                {"submodule/v2", "root/go.mod", true},
                {"submodule/v2", "root/other/go.mod", true},
        };
    }

    // --- isVendorPackage unit tests (from Go's vendor_test.go) ---

    @Test(dataProvider = "vendorPackageProvider")
    public void testIsVendorPackage(String goModVersion, String entryName, boolean expected) throws Exception {
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "1.0.0", null);
        streamer.goModVersion = Version.parse(goModVersion);

        Method method = GoZipBallStreamer.class.getDeclaredMethod("isVendorPackage", String.class);
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(streamer, entryName);

        assertEquals(result, expected, "Failed for: " + entryName + " with Go version: " + goModVersion);
    }

    @DataProvider
    public Object[][] vendorPackageProvider() {
        return new Object[][]{
                //https://cs.opensource.google/go/x/mod/+/c8a731972177c6ce4073699c705e55918ee7be09:zip/vendor_test.go

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

                {"1.23.0", "vendor/modules.txt", false},
                {"1.24.0", "vendor/modules.txt", true},
                {"1.25.0", "vendor/modules.txt", true},
                {"1.23.0", "foo/vendor/modules.txt", false},
                {"1.24.0", "foo/vendor/modules.txt", true},
                {"1.25.0", "foo/vendor/modules.txt", true},

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

    // --- setSubModuleNameExplicitly unit tests ---

    @Test
    void testSetSubModuleNameExplicitlySetFlag() throws Exception {
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "v1.0.0", log);

        Field explicitlySetField = GoZipBallStreamer.class.getDeclaredField("subModuleNameExplicitlySet");
        explicitlySetField.setAccessible(true);
        assertFalse((boolean) explicitlySetField.get(streamer));

        streamer.setSubModuleNameExplicitly("submodule");
        assertTrue((boolean) explicitlySetField.get(streamer));

        streamer.setSubModuleNameExplicitly("");
        assertTrue((boolean) explicitlySetField.get(streamer));
    }

    @Test
    void testSetSubModuleNameExplicitlyHandlesNull() throws Exception {
        GoZipBallStreamer streamer = new GoZipBallStreamer(null, "project", "v1.0.0", log);
        streamer.setSubModuleNameExplicitly(null);

        Field explicitlySetField = GoZipBallStreamer.class.getDeclaredField("subModuleNameExplicitlySet");
        explicitlySetField.setAccessible(true);
        assertTrue((boolean) explicitlySetField.get(streamer));
    }

    // --- hasModFileAtSubModulePath edge cases (synthetic zips not covered by packing tests) ---

    @Test
    void hasModFileAtSubModulePath_emptyZip_returnsFalse() throws Exception {
        File tempFile = File.createTempFile("empty-test", ".zip");
        tempFile.deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            zos.finish();
        }
        ZipFile zipFile = new ZipFile(tempFile);
        GoZipBallStreamer streamer = new GoZipBallStreamer(zipFile,
                "github.com/owner/repo/foo/v2", "v2.1.0", log);

        assertFalse(invokeHasModFileAtSubModulePath(streamer, "foo/v2"),
                "Empty zip should return false");
    }

    @Test
    void hasModFileAtSubModulePath_noTopLevelDir_returnsFalse() throws Exception {
        File tempFile = File.createTempFile("flat-test", ".zip");
        tempFile.deleteOnExit();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile))) {
            zos.putNextEntry(new ZipEntry("go.mod"));
            zos.write("module example\n".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        ZipFile zipFile = new ZipFile(tempFile);
        GoZipBallStreamer streamer = new GoZipBallStreamer(zipFile,
                "github.com/owner/repo/foo/v2", "v2.1.0", log);

        assertFalse(invokeHasModFileAtSubModulePath(streamer, "foo/v2"),
                "Zip with no top-level directory should return false");
    }

    // --- initiateProjectType (only cases not covered by packing tests) ---

    @Test
    void initiateProjectType_rootV2Module_noSubModule() throws Exception {
        ZipFile zipFile = getZipFile("/go/test-nested-submodule-v2-root-v2.zip");
        GoZipBallStreamer streamer = new GoZipBallStreamer(zipFile,
                "github.com/thepudds/nested-module-example/v2", "v2.0.0", log);

        invokeInitiateProjectType(streamer);

        assertEquals("", getSubModuleName(streamer),
                "Root v2 module: subModuleName should be empty (pack from root)");
    }

    // --- End-to-end packing tests ---

    @Test(dataProvider = "providePackingData")
    void packingZipTest(String rootRepoZip, String projectName, String version, String expResZip)
            throws IOException {
        GoZipBallStreamer streamer = new GoZipBallStreamer(
                getZipFile("/go/" + rootRepoZip), projectName, version, log);
        File targetFile = File.createTempFile("go-packing-test-", ".zip");
        targetFile.deleteOnExit();
        streamer.writeDeployableZip(targetFile);
        assertZipFilesEquality("/go/" + expResZip, targetFile.getPath());
    }

    @DataProvider
    private Object[][] providePackingData() {
        return new Object[][]{
                {"project-with-submodule.zip", "github.com/owner/project", "v1.1.7", "res-root-module.zip"},
                {"testvendor-go23.zip", "github.com/dorsJfrog/testvendor", "v1.0.4", "res-testvendor-go23.zip"},
                {"test-submodule-v2.zip", "github.com/dorsJfrog/gosubmodule/submodule/v2",
                        "v2.0.0", "res-submodule-v2.zip"},
                {"test-submodule-v2-branch-layout.zip", "github.com/owner/repo/foo/v2",
                        "v2.1.0", "res-submodule-v2-branch-layout.zip"},
                {"test-nested-module-v2.zip", "github.com/thepudds/nested-module-example/contrib/nested1/v2",
                        "v2.0.0", "res-nested-module-v2.zip"},
                {"test-nested-submodule-v2-root-v2.zip",
                        "github.com/thepudds/nested-module-example/contrib/nested1/v2",
                        "v2.0.0", "res-nested-submodule-v2-root-v2.zip"},
                {"testvendor-go24.zip", "github.com/dorsJfrog/testvendor", "v1.0.2", "res-testvendor-go24.zip"},
                {"test-modulesLIC.zip", "github.com/p4r53c/go-lic-repro/pkg/module1",
                        "v0.1.0", "res-module1WithLIC.zip"},
                {"test-modulesLIC.zip", "github.com/p4r53c/go-lic-repro/pkg/module2",
                        "v0.1.0", "res-module2withoutLIC.zip"},
                {"project-with-submodule.zip", "github.com/owner/project/some/submodule", "v1.1.7",
                        "res-submodule.zip"},
                {"versionsTest-master.zip", "github.com/owner/versionsTest", "v1.5.0",
                        "compatible-version-test-v1.zip"},
                {"versionsTest-master.zip", "github.com/owner/versionsTest/v2", "v2.4.0",
                        "compatible-version-test-v2.zip"},
                {"excluded-files-test-v1.zip", "github.com/example/hello-hash",
                        "v0.0.0-20180517005709-c856192", "res-excluded-files-test-v1.zip"},
                {"complex-project.zip", "github.com/complex-project/firstModule", "v1.0.6",
                        "res-complex-project-firstModule.zip"},
                {"complex-project.zip", "github.com/complex-project/secModule/secModule", "v1.0.6",
                        "res-complex-project-secModule.zip"},
                {"helloGoV2CompatibleRootProject.zip", "github.com/jfrog-qa/helloGoV2CompatibleRootProject/v3",
                        "v3.0.0", "res-helloGoV2CompatibleRootProject.zip"},
                {"test-picksubmodule.zip", "github.com/owner/go-example/hello", "v1.0.0",
                        "res-picksubmodule.zip"},
                {"test-simple-root-module.zip", "github.com/owner/hello", "v1.0.0",
                        "res-simple-root-module.zip"},
                {"test-incompatible-v2.zip", "github.com/owner/repo", "v2.0.0+incompatible",
                        "res-incompatible-v2.zip"},
                {"test-gopkg-in-v2.zip", "gopkg.in/yaml.v2", "v2.4.0", "res-gopkg-in-v2.zip"},
        };
    }

    @Test(dataProvider = "provideGitLabPackingData")
    void packingZipTestGitLab(String rootRepoZip, String projectName,
            String submodule, String version, String expResZip) throws IOException {
        GoZipBallStreamer streamer = new GoZipBallStreamer(
                getZipFile("/go/" + rootRepoZip), projectName, version, log);
        streamer.setSubModuleNameExplicitly(submodule);
        File targetFile = File.createTempFile("go-packing-test-gitlab-", ".zip");
        targetFile.deleteOnExit();
        streamer.writeDeployableZip(targetFile);
        assertZipFilesEquality("/go/" + expResZip, targetFile.getPath());
    }

    @DataProvider
    private Object[][] provideGitLabPackingData() {
        return new Object[][]{
                {"test-gitlab-major-at-root.zip", "gitlab.com/techme-group/testp/v2",
                        "v2", "v2.0.8", "res-gitlab-major-at-root.zip"},
                {"test-gitlab-submodule-v2.zip", "gitlab.com/jfrog-qa3/go-test-module/v2",
                        "v2", "v2.0.0", "res-gitlab-submodule-v2.zip"},
                {"project-with-submodule.zip", "gitlab.com/group/subgroup/project",
                        "", "v1.1.7", "res-gitlab-explicit-empty-submodule.zip"},
                {"project-with-submodule.zip", "gitlab.com/group/project",
                        "some/submodule", "v1.1.7", "res-gitlab-explicit-submodule.zip"},
        };
    }

    // --- scanEntries submodule root detection (from RTDEV-82256) ---

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
        Map<String, String> basicSubModuleEntries = new LinkedHashMap<>();
        basicSubModuleEntries.put("myrepo-v1.0.0/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/root.go", "package main\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/submod/go.mod", "module github.com/owner/repo/submod\n\ngo 1.21.0\n");
        basicSubModuleEntries.put("myrepo-v1.0.0/submod/submod.go", "package submod\n");

        Map<String, String> nestedSubModuleEntries = new LinkedHashMap<>();
        nestedSubModuleEntries.put("repo-abc123/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        nestedSubModuleEntries.put("repo-abc123/util/util.go", "package util\n");
        nestedSubModuleEntries.put("repo-abc123/lib/go.mod", "module github.com/owner/repo/lib\n\ngo 1.21.0\n");
        nestedSubModuleEntries.put("repo-abc123/lib/lib.go", "package lib\n");
        nestedSubModuleEntries.put("repo-abc123/lib/helper/helper.go", "package helper\n");

        Map<String, String> subModuleWithLicenseEntries = new LinkedHashMap<>();
        subModuleWithLicenseEntries.put("proj-v2.0.0/go.mod", "module github.com/owner/repo\n\ngo 1.21.0\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/LICENSE", "MIT License\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/main.go", "package main\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/submod/go.mod", "module github.com/owner/repo/submod\n\ngo 1.21.0\n");
        subModuleWithLicenseEntries.put("proj-v2.0.0/submod/api.go", "package submod\n");

        Map<String, String> ambiguousSubModuleEntries = new LinkedHashMap<>();
        ambiguousSubModuleEntries.put("go-example-abc123/go.mod", "module github.com/owner/go-example\n\ngo 1.21.0\n");
        ambiguousSubModuleEntries.put("go-example-abc123/main.go", "package main\n");
        ambiguousSubModuleEntries.put("go-example-abc123/docs/godocs/hello/README.md", "# Hello docs\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/go.mod", "module github.com/owner/go-example/hello\n\ngo 1.21.0\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/hello.go", "package hello\n");
        ambiguousSubModuleEntries.put("go-example-abc123/hello/world/world.go", "package world\n");

        return new Object[][]{
                {
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

    // --- Helpers ---

    private void assertZipFilesEquality(String expectedResourcePath, String actualPath) throws IOException {
        ZipFile expectedZip = getZipFile(expectedResourcePath);
        ZipFile actualZip = new ZipFile(actualPath);
        Enumeration<ZipArchiveEntry> expectedEntries = expectedZip.getEntries();
        Enumeration<ZipArchiveEntry> actualEntries = actualZip.getEntries();
        Set<String> expectedSet = new LinkedHashSet<>();
        Set<String> actualSet = new LinkedHashSet<>();

        while (expectedEntries.hasMoreElements()) {
            expectedSet.add(expectedEntries.nextElement().getName());
        }
        while (actualEntries.hasMoreElements()) {
            actualSet.add(actualEntries.nextElement().getName());
        }
        expectedSet.forEach(name -> {
            assertTrue(actualSet.contains(name), "Expected entry missing from actual zip: " + name);
            actualSet.remove(name);
            try {
                assertTrue(IOUtils.contentEquals(
                        expectedZip.getInputStream(expectedZip.getEntry(name)),
                        actualZip.getInputStream(actualZip.getEntry(name))),
                        "Content mismatch for entry: " + name);
            } catch (IOException e) {
                throw new RuntimeException("Failed to compare entry: " + name, e);
            }
        });
        assertTrue(actualSet.isEmpty(), "Unexpected entries in actual zip: " + actualSet);
    }

    private static ZipFile getZipFile(String path) {
        try {
            URL resource = GoZipBallStreamerTest.class.getResource(path);
            if (resource == null) {
                throw new IllegalArgumentException("Test resource not found: " + path);
            }
            return new ZipFile(new File(resource.toURI()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load test zip: " + path, e);
        }
    }

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

    private static void invokeInitiateProjectType(GoZipBallStreamer streamer) throws Exception {
        Method method = GoZipBallStreamer.class.getDeclaredMethod("initiateProjectType");
        method.setAccessible(true);
        method.invoke(streamer);
    }

    private static String getSubModuleName(GoZipBallStreamer streamer) throws Exception {
        Field field = GoZipBallStreamer.class.getDeclaredField("subModuleName");
        field.setAccessible(true);
        return (String) field.get(streamer);
    }

    private static boolean invokeHasModFileAtSubModulePath(GoZipBallStreamer streamer, String subModulePath)
            throws Exception {
        Method method = GoZipBallStreamer.class.getDeclaredMethod("hasModFileAtSubModulePath", String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(streamer, subModulePath);
    }
}
