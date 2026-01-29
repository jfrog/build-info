package org.jfrog.build.extractor.go.extractor;

import com.github.zafarkhaja.semver.Version;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.api.util.NullLog;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.lang.reflect.Field;
import java.lang.reflect.Method;


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
        streamer.goModVersion = Version.valueOf(goModVersion);

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

}