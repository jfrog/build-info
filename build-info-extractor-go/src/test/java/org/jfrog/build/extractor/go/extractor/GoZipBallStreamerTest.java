package org.jfrog.build.extractor.go.extractor;

import com.github.zafarkhaja.semver.Version;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.lang.reflect.Method;


public class GoZipBallStreamerTest {
    @Test(dataProvider = "testIsSubModuleProvider")
    public void testIsSubModule(String subModuleName, String entryName, boolean expectedResult) {
        GoZipBallStreamer goZipBallStreamer = new GoZipBallStreamer(null, "ignore", "ignore", null);
        goZipBallStreamer.setSubModuleName(subModuleName);
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

}