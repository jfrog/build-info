/*
 * Copyright (C) 2011 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jfrog.build.extractor.maven.transformer;

import org.jfrog.build.api.util.CommonUtils;
import org.jfrog.build.extractor.maven.reader.ModuleName;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.*;


/**
 * Tests the pom version change transformations.
 *
 * @author Yossi Shaul
 */
public class PomTransformerTest {

    private static final String pomHeader = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">";

    @Test
    public void transformSimplePom() throws Exception {
        File pomFile = getResourceAsFile("/poms/parentonly/pom.xml");
        HashMap<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "parent"), "2.2");

        new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "").transform(pomFile);

        String pomStr = getFileAsString(pomFile);
        File expectedFile = getResourceAsFile("/poms/parentonly/pom.xml");
        String expectedStr = getFileAsString(expectedFile);

        assertEquals(pomStr, expectedStr);
    }

    @Test
    public void transformMultiPom() throws Exception {
        File pomFile = getResourceAsFile("/poms/multi/pom.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "two"), "3.6");

        new PomTransformer(new ModuleName("org.jfrog.test.nested", "two"), modules, "").transform(pomFile);
        String pomStr = getFileAsString(pomFile);

        File expectedFile = getResourceAsFile("/poms/multi/pom.expected.xml");
        String expectedStr = getFileAsString(expectedFile);

        assertEquals(pomStr, expectedStr);
    }

    @Test
    public void transformGitScm() throws Exception {
        File pomFile = getResourceAsFile("/poms/scm/git/pom.xml");
        HashMap<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "parent"), "1");

        new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, null).transform(pomFile);

        String pomStr = getFileAsString(pomFile);
        String expectedStr = getFileAsString(getResourceAsFile("/poms/scm/git/pom.expected.xml"));
        assertEquals(pomStr, expectedStr);
    }

    @Test
    public void transformSvnScm() throws Exception {
        File pomFile = getResourceAsFile("/poms/scm/svn/pom.xml");
        HashMap<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "parent"), "1");

        new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules,
                "http://subversion.jfrog.org/test/tags/1").transform(pomFile);

        String pomStr = getFileAsString(pomFile);
        String expectedStr = getFileAsString(getResourceAsFile("/poms/scm/svn/pom.expected.xml"));
        assertEquals(pomStr, expectedStr);
    }

    @Test
    public void snapshotsModule() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot.xml");
        String message = snapshotModule(pomFile, "2.2-SNAPSHOT");
        assertTrue(message.contains("org.jfrog.test:one:2.2-SNAPSHOT"), "Unexpected error message: " + message);
    }

    @Test
    public void snapshotsPropertiesModule() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot-properties.xml");
        String message = snapshotModule(pomFile, "2.2");
        assertTrue(message.contains("org.jfrog.test:parent:${version.test}"), "Unexpected error message: " + message);
    }

    @Test
    public void snapshotsPropertiesAtParentModule() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot-properties-at-parent.xml");
        String message = snapshotModule(pomFile, "2.1");
        assertTrue(message.contains("org.jfrog.test:parent:${version.test}"), "Unexpected error message: " + message);
    }

    @Test
    public void snapshotsInParent() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshot-parent.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        try {
            new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "", true).transform(pomFile);
            fail("Pom contains snapshot in the parent and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue(message.contains("org.jfrog.test:parent:2.1-SNAPSHOT"),
                    "Unexpected error message: " + message);
        }
    }

    @Test
    public void snapshotsInDependenciesManagement() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshots-in-dep-management.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "four"), "3.6");

        try {
            new PomTransformer(new ModuleName("org.jfrog.test.nested", "four"), modules, "", true).transform(pomFile);
            fail("Pom contains snapshot in the dependency management and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue(message.contains("org.jfrog.test.nested:nestedX:2.0-SNAPSHOT"),
                    "Unexpected error message: " + message);
        }
    }

    @Test
    public void snapshotsInDependencies() throws Exception {
        File pomFile = getResourceAsFile("/poms/snapshots/pom-snapshots-in-dependencies.xml");
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test.nested", "nested1"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "nested2"), "3.6");
        modules.put(new ModuleName("org.jfrog.test.nested", "four"), "3.6");

        try {
            new PomTransformer(new ModuleName("org.jfrog.test.nested", "four"), modules, "", true).transform(pomFile);
            fail("Pom contains snapshot in the dependencies and should fail");
        } catch (SnapshotNotAllowedException e) {
            String message = e.getMessage();
            assertTrue(message.contains("org.jfrog.test.nested:nestedX:3.2-SNAPSHOT"),
                    "Unexpected error message: " + message);
        }
    }

    @Test
    public void testCrEolArePreserved() throws Exception {
        String transformedValue = transformPomWithEol("\r");
        assertTrue(transformedValue.contains("\r"));
        assertFalse(transformedValue.contains("\n"));
    }

    @Test
    public void testLfEolArePreserved() throws Exception {
        String transformedValue = transformPomWithEol("\n");
        assertFalse(transformedValue.contains("\r"));
        assertTrue(transformedValue.contains("\n"));
    }

    @Test
    public void testCrLfEolArePreserved() throws Exception {
        String transformedValue = transformPomWithEol("\r\n");
        assertTrue(transformedValue.contains("\r"));
        assertTrue(transformedValue.contains("\n"));
    }

    private String getPomContent(String eol) {
        return new StringBuilder(pomHeader).append(eol).append("<modelVersion>4.0.0</modelVersion>").append(eol)
                .append("<groupId>group</groupId>").append(eol).append("<artifactId>artifact</artifactId>").append(eol)
                .append("<version>111</version>").append(eol).append("</project>").toString();
    }

    private File getResourceAsFile(String path) {
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.getFile());
    }

    private String transformPomWithEol(String eol) throws IOException {
        File file = File.createTempFile("temp", "pom");
        CommonUtils.writeByCharset(getPomContent(eol), file, Charset.defaultCharset());
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("group", "artifact"), "112");
        new PomTransformer(new ModuleName("group", "artifact"), modules, "").transform(file);
        return getFileAsString(file);
    }

    private String getFileAsString(File file) throws IOException {
        return CommonUtils.readByCharset(file, Charset.defaultCharset());
    }

    private String snapshotModule(File pomFile, String moduleVersion) throws Exception {
        Map<ModuleName, String> modules = new HashMap<>();
        modules.put(new ModuleName("org.jfrog.test", "one"), moduleVersion);
        try {
            new PomTransformer(new ModuleName("org.jfrog.test", "one"), modules, "", true).transform(pomFile);
            fail("Pom contains module with snapshot version and should fail");
        } catch (SnapshotNotAllowedException e) {
            return e.getMessage();
        }
        return "";
    }
}
