package org.jfrog.build.extractor.npm.types;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class NpmPackageInfoTest {

    @DataProvider
    private Object[][] moduleNamesProvider() {
        return new Object[][]{
                // name | version | scope | moduleId | packedFileName | deployPath
                {"stark", "mark", "80", "stark:mark:80", "stark-mark-80.tgz", "stark/mark/-/mark-80.tgz"},
                {"@stark", "mark", "81", "stark:mark:81", "stark-mark-81.tgz", "@stark/mark/-/mark-81.tgz"},
                {"", "mark", "82", "mark:82", "mark-82.tgz", "mark/-/mark-82.tgz"},
                {null, "mark", "83", "mark:83", "mark-83.tgz", "mark/-/mark-83.tgz"}
        };
    }

    @Test(dataProvider = "moduleNamesProvider")
    public void testModuleNames(String scope, String name, String version, String expectedModuleId, String expectedPackedFileName, String expectedDeployPath) {
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, version, scope, null);
        assertEquals(npmPackageInfo.getModuleId(), expectedModuleId);
        assertEquals(npmPackageInfo.getExpectedPackedFileName(), expectedPackedFileName);
        assertEquals(npmPackageInfo.getDeployPath(), expectedDeployPath);
    }

    @DataProvider
    private Object[][] removeVersionPrefixesProvider() {
        return new Object[][]{
                // version | expectedVersion
                {"", ""},
                {"80", "80"},
                {"v81", "81"},
                {"=82", "82"},
                {"=v83", "83"}
        };
    }

    @Test(dataProvider = "removeVersionPrefixesProvider")
    public void testRemoveVersionPrefixes(String version, String expectedVersion) {
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo("", version, "", null);
        npmPackageInfo.removeVersionPrefixes();
        assertEquals(npmPackageInfo.getVersion(), expectedVersion);
    }

    @DataProvider
    private Object[][] splitScopeFromNameProvider() {
        return new Object[][]{
                // scope | name | expectedScope | expectedName
                {"", "", "", ""},
                {"stark", "", "stark", ""},
                {"", "mark", "", "mark"},
                {"stark", "mark", "stark", "mark"},
                {"cave", "@cave/mark", "@cave", "mark"},
                {"stark", "@cave/mark", "@cave", "mark"},
                {"stark", "cave/mark", "stark", "cave/mark"}
        };
    }

    @Test(dataProvider = "splitScopeFromNameProvider")
    public void testSplitScopeFromName(String scope, String name, String expectedScope, String expectedName) {
        NpmPackageInfo npmPackageInfo = new NpmPackageInfo(name, "", scope, null);
        npmPackageInfo.splitScopeFromName();
        assertEquals(npmPackageInfo.getScope(), expectedScope);
        assertEquals(npmPackageInfo.getName(), expectedName);
    }

    @DataProvider
    private Object[][] readPackageInfoProvider() {
        return new Object[][]{
                // scope | name | version
                {"@stark/mark", "80", "@stark", "mark", "80"},
                {"@stark/mark", "@81", "@stark", "mark", "@81"},
                {"@stark/mark", "", "@stark", "mark", ""},
                {"stark/mark", "82", null, "stark/mark", "82"},
                {"stark", "83", null, "stark", "83"},
                {"", "84", null, "", "84"},
                {"", "", null, "", ""}
        };
    }

    @Test(dataProvider = "readPackageInfoProvider")
    public void readPackageInfoTest(String name, String version, String expectedScope, String expectedName, String expectedVersion) {
        ObjectMapper objectMapper = new ObjectMapper();
        NpmPackageInfo inputPackageInfo = new NpmPackageInfo(name, version, "", null);
        try (InputStream is = new ByteArrayInputStream(objectMapper.writeValueAsBytes(inputPackageInfo))) {
            NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
            npmPackageInfo.readPackageInfo(is);
            assertEquals(npmPackageInfo.getScope(), expectedScope);
            assertEquals(npmPackageInfo.getName(), expectedName);
            assertEquals(npmPackageInfo.getVersion(), expectedVersion);
        } catch (IOException e) {
            fail(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }

    @DataProvider
    private Object[][] readPackageInfoPathToRootProvider() {
        return new Object[][]{
                // name | version | pathToRoot
                {"debug", "2.6.9", new String[]{"send:0.16.2", "npm-example:0.0.3"}},
                {"debug", "4.3.1", new String[]{"npm-example:0.0.3"}},
        };
    }

    @Test(dataProvider = "readPackageInfoPathToRootProvider")
    public void readPackageInfoPathToRoot(String name, String version, String[] pathToRoot) {
        ObjectMapper objectMapper = new ObjectMapper();
        NpmPackageInfo inputPackageInfo = new NpmPackageInfo(name, version, "", pathToRoot);
        try (InputStream is = new ByteArrayInputStream(objectMapper.writeValueAsBytes(inputPackageInfo))) {
            NpmPackageInfo npmPackageInfo = new NpmPackageInfo();
            npmPackageInfo.readPackageInfo(is);
            assertEquals(npmPackageInfo.getPathToRoot(), pathToRoot);
            assertEquals(npmPackageInfo.getName(), name);
        } catch (IOException e) {
            fail(ExceptionUtils.getRootCauseMessage(e), e);
        }
    }
}