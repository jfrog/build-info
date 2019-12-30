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

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jfrog.build.extractor.EolDetectingInputStream;
import org.jfrog.build.extractor.maven.reader.ModuleName;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Rewrites the project versions in the pom.
 *
 * @author Yossi Shaul
 */
public class PomTransformer {

    private final String UNIX_DEFAULT_PARENT_POM_RELATIVE_PATH = "../pom.xml";
    private final String WINDOWS_DEFAULT_PARENT_POM_RELATIVE_PATH = "..\\pom.xml";
    private final String scmUrl;
    private final ModuleName currentModule;
    private final Map<ModuleName, String> versionsByModule;
    private final boolean failOnSnapshot;
    private final boolean dryRun;

    private boolean modified;
    private File pomFile;
    private Map<String, String> pomProperties = new HashMap<String, String>();
    private String nextPomToLoad = null;

    /**
     * Transforms single pom file.
     *
     * @param currentModule    The current module we work on
     * @param versionsByModule Map of module names to module version
     * @param scmUrl           Scm url to use if scm element exists in the pom file
     */
    public PomTransformer(ModuleName currentModule, Map<ModuleName, String> versionsByModule, String scmUrl) {
        this(currentModule, versionsByModule, scmUrl, false, false);
    }

    /**
     * Transforms single pom file.
     *
     * @param currentModule    The current module we work on
     * @param versionsByModule Map of module names to module version
     * @param scmUrl           Scm url to use if scm element exists in the pom file
     * @param failOnSnapshot   If true, fail with IllegalStateException if the pom contains snapshot version after the
     *                         version changes
     */
    public PomTransformer(ModuleName currentModule, Map<ModuleName, String> versionsByModule, String scmUrl,
                          boolean failOnSnapshot) {
        this(currentModule, versionsByModule, scmUrl, failOnSnapshot, false);
    }

    /**
     * Transforms single pom file.
     *
     * @param currentModule    The current module we work on
     * @param versionsByModule Map of module names to module version
     * @param scmUrl           Scm url to use if scm element exists in the pom file
     * @param failOnSnapshot   If true, fail with IllegalStateException if the pom contains snapshot version after the
     *                         version changes
     * @param dryRun           If true, changes will not take effect.
     */
    public PomTransformer(ModuleName currentModule, Map<ModuleName, String> versionsByModule, String scmUrl,
                          boolean failOnSnapshot, boolean dryRun) {
        this.currentModule = currentModule;
        this.versionsByModule = versionsByModule;
        this.scmUrl = scmUrl;
        this.failOnSnapshot = failOnSnapshot;
        this.dryRun = dryRun;
    }

    /**
     * Performs the transformation.
     *
     * @return True if the file was modified.
     */
    public Boolean transform(File pomFile) throws IOException {
        this.pomFile = pomFile;
        if (!pomFile.exists()) {
            throw new IllegalArgumentException("Couldn't find pom file: " + pomFile);
        }

        SAXBuilder saxBuilder = createSaxBuilder();
        Document document;
        EolDetectingInputStream eolDetectingStream = null;
        InputStreamReader inputStreamReader = null;
        try {
            eolDetectingStream = new EolDetectingInputStream(new FileInputStream(pomFile));
            inputStreamReader = new InputStreamReader(eolDetectingStream, "UTF-8");
            document = saxBuilder.build(inputStreamReader);
        } catch (JDOMException e) {
            throw new IOException("Failed to parse pom: " + pomFile.getAbsolutePath(), e);
        } finally {
            IOUtils.closeQuietly(inputStreamReader);
            IOUtils.closeQuietly(eolDetectingStream);
        }

        Element rootElement = document.getRootElement();
        Namespace ns = rootElement.getNamespace();
        getProperties(rootElement, ns);
        changeParentVersion(rootElement, ns);

        changeCurrentModuleVersion(rootElement, ns);

        //changePropertiesVersion(rootElement, ns);

        changeDependencyManagementVersions(rootElement, ns);

        changeDependencyVersions(rootElement, ns);

        if (scmUrl != null) {
            changeScm(rootElement, ns);
        }

        if (modified && !dryRun) {
            FileOutputStream fileOutputStream = new FileOutputStream(pomFile);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, "UTF-8");
            try {
                XMLOutputter outputter = new XMLOutputter();
                String eol = eolDetectingStream.getEol();
                if (!"".equals(eol)) {
                    Format format = outputter.getFormat();
                    format.setLineSeparator(eol);
                    format.setTextMode(Format.TextMode.PRESERVE);
                    outputter.setFormat(format);
                }
                outputter.output(document, outputStreamWriter);
            } finally {
                IOUtils.closeQuietly(outputStreamWriter);
                IOUtils.closeQuietly(fileOutputStream);
            }
        }

        return modified;
    }

    private void getProperties(Element root, Namespace ns) {
        Element propertiesElement = root.getChild("properties", ns);
        if (propertiesElement == null) {
            return;
        }

        List<Element> children = propertiesElement.getChildren();
        for (Element child : children) {
            String key = child.getName();
            if (pomProperties.get(key) == null) {
                pomProperties.put(key, child.getText());
            }
        }
    }

    private void changeParentVersion(Element root, Namespace ns) {
        Element parentElement = root.getChild("parent", ns);
        if (parentElement == null) {
            return;
        }
        // We might need to use the parent pom for getting properties
        // If the file has parent but relativePath is not set we use the default parent location
        Element relativePath = parentElement.getChild("relativePath", ns);
        String relativeParentPath = relativePath == null ? ".." + java.io.File.separator + "pom.xml" : relativePath.getText();
        nextPomToLoad = StringUtils.substringBeforeLast(pomFile.getAbsolutePath(), java.io.File.separator) + java.io.File.separator + relativeParentPath;
        ModuleName parentName = extractModuleName(parentElement, ns);
        if (versionsByModule.containsKey(parentName)) {
            setVersion(parentElement, ns, versionsByModule.get(parentName));
        }
        verifyNonSnapshotVersion(parentName, parentElement, ns);
    }

    private void changeCurrentModuleVersion(Element rootElement, Namespace ns) {
        setVersion(rootElement, ns, versionsByModule.get(currentModule));
        verifyNonSnapshotVersion(currentModule, rootElement, ns);
    }

    private void changeDependencyManagementVersions(Element rootElement, Namespace ns) {
        Element dependencyManagement = rootElement.getChild("dependencyManagement", ns);
        if (dependencyManagement == null) {
            return;
        }

        Element dependenciesElement = dependencyManagement.getChild("dependencies", ns);
        if (dependenciesElement == null) {
            return;
        }

        List<Element> dependencies = dependenciesElement.getChildren("dependency", ns);
        for (Element dependency : dependencies) {
            changeDependencyVersion(ns, dependency);
        }
    }

    private void changeDependencyVersions(Element rootElement, Namespace ns) {
        Element dependenciesElement = rootElement.getChild("dependencies", ns);
        if (dependenciesElement == null) {
            return;
        }

        List<Element> dependencies = dependenciesElement.getChildren("dependency", ns);
        for (Element dependency : dependencies) {
            changeDependencyVersion(ns, dependency);
        }
    }

    private void changeDependencyVersion(Namespace ns, Element dependency) {
        ModuleName moduleName = extractModuleName(dependency, ns);
        if (versionsByModule.containsKey(moduleName)) {
            setVersion(dependency, ns, versionsByModule.get(moduleName));
        }
        verifyNonSnapshotVersion(moduleName, dependency, ns);
    }

    private void changeScm(Element rootElement, Namespace ns) {
        Element scm = rootElement.getChild("scm", ns);
        if (scm == null) {
            return;
        }
        Element connection = scm.getChild("connection", ns);
        if (connection != null) {
            connection.setText("scm:svn:" + scmUrl);
        }
        Element developerConnection = scm.getChild("developerConnection", ns);
        if (developerConnection != null) {
            developerConnection.setText("scm:svn:" + scmUrl);
        }
        Element url = scm.getChild("url", ns);
        if (url != null) {
            url.setText(scmUrl);
        }
    }

    private void setVersion(Element element, Namespace ns, String version) {
        Element versionElement = element.getChild("version", ns);
        if (versionElement != null) {
            String currentVersion = versionElement.getText();
            if (!version.equals(currentVersion)) {
                versionElement.setText(version);
                modified = true;
            }
        }
    }

    private void verifyNonSnapshotVersion(ModuleName moduleName, Element element, Namespace ns) {
        if (!failOnSnapshot) {
            return;
        }
        Element versionElement = element.getChild("version", ns);
        if (versionElement != null) {
            String currentVersion = versionElement.getText();
            if (currentVersion.endsWith("-SNAPSHOT") ||
                    (currentVersion.startsWith("${") && currentVersion.endsWith("}")) && evalExpression(currentVersion) != null && evalExpression(currentVersion).endsWith("-SNAPSHOT")) {
                throw new SnapshotNotAllowedException(String.format("Snapshot detected in file '%s': %s:%s",
                        pomFile.getAbsolutePath(), moduleName, currentVersion));
            }
        }
    }

    // Search for expression value in the pom properties
    // If not found, try to find the value up in the pom hierarchy, lazy loading
    private String evalExpression(String expr) {
        String expression = pomProperties.get(expr.substring(2, expr.lastIndexOf("}")));
        if (expression != null) {
            return expression;
        }

        if (loadNextProperties()) {
            return evalExpression(expr);
        }

        return null;
    }

    // The nextPomToLoad point to the next pom hierarchy
    // load the next pom in chain and his properties
    private boolean loadNextProperties() {
        if (nextPomToLoad == null) {
            return false;
        }

        MavenXpp3Reader mavenReader = new MavenXpp3Reader();
        try (FileReader reader = new FileReader(nextPomToLoad)) {
            Model model = mavenReader.read(reader);
            Properties properties = model.getProperties();
            for (String key : properties.stringPropertyNames()) {
                if (pomProperties.get(key) == null) {
                    pomProperties.put(key, properties.getProperty(key));
                }
            }

            Parent parent = model.getParent();
            if (parent != null) {
                String relativePath = parent.getRelativePath();
                if (File.separatorChar == '\\' && UNIX_DEFAULT_PARENT_POM_RELATIVE_PATH.equals(relativePath)) {
                    relativePath = WINDOWS_DEFAULT_PARENT_POM_RELATIVE_PATH;
                }
                nextPomToLoad = StringUtils.substringBeforeLast(nextPomToLoad, File.separator) + File.separator + relativePath;
            } else {
                nextPomToLoad = null;
            }
        } catch (Exception e) {
            Logger.getLogger(PomTransformer.class.getName()).info("couldn't load pom file at: " + nextPomToLoad);
            return false;
        }
        return true;
    }

    private ModuleName extractModuleName(Element element, Namespace ns) {
        String groupId = element.getChildText("groupId", ns);
        String artifactId = element.getChildText("artifactId", ns);
        if (StringUtils.isBlank(groupId) || StringUtils.isBlank(artifactId)) {
            throw new IllegalArgumentException("Couldn't extract module key from: " + element);
        }
        return new ModuleName(groupId, artifactId);
    }


    static SAXBuilder createSaxBuilder() {
        SAXBuilder sb = new SAXBuilder();
        // don't validate and don't load dtd
        sb.setValidation(false);
        sb.setFeature("http://xml.org/sax/features/validation", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        sb.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        return sb;
    }
}
