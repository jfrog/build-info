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

package org.jfrog.build.extractor.maven;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoRecorder.class)
public class BuildInfoRecorder extends AbstractExecutionListener implements BuildInfoExtractor<ExecutionEvent, Build> {

    @Requirement
    private Logger logger;

    @Requirement
    private BuildInfoModelPropertyResolver buildInfoModelPropertyResolver;

    @Requirement
    private BuildDeploymentHelper buildDeploymentHelper;

    private ExecutionListener wrappedListener;
    private BuildInfoMavenBuilder buildInfoBuilder;
    private ThreadLocal<ModuleBuilder> currentModule = new ThreadLocal<ModuleBuilder>();
    private ThreadLocal<Set<Artifact>> currentModuleArtifacts = new ThreadLocal<Set<Artifact>>();
    private ThreadLocal<Set<Artifact>> currentModuleDependencies = new ThreadLocal<Set<Artifact>>();
    private volatile boolean projectHasTestFailures;

    private Map<String, DeployDetails> deployableArtifactBuilderMap;
    private ArtifactoryClientConfiguration conf;
    private Map<String, String> matrixParams;

    public void setListenerToWrap(ExecutionListener executionListener) {
        wrappedListener = executionListener;
    }

    public void setConfiguration(ArtifactoryClientConfiguration conf) {
        this.conf = conf;
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectDiscoveryStarted(event);
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        logger.info("Initializing Artifactory Build-Info Recording");
        buildInfoBuilder = buildInfoModelPropertyResolver.resolveProperties(event, conf);
        deployableArtifactBuilderMap = Maps.newHashMap();
        matrixParams = Maps.newHashMap();
        Map<String, String> matrixParamProps = conf.publisher.getMatrixParams();
        for (Map.Entry<String, String> matrixParamProp : matrixParamProps.entrySet()) {
            String key = matrixParamProp.getKey();
            key = StringUtils.removeStartIgnoreCase(key, ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX);
            matrixParams.put(key, matrixParamProp.getValue());
        }

        if (wrappedListener != null) {
            wrappedListener.sessionStarted(event);
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        Build build = extract(event);
        if (build != null) {
            File basedir = event.getSession().getTopLevelProject().getBasedir();
            buildDeploymentHelper.deploy(build, conf, deployableArtifactBuilderMap, projectHasTestFailures, basedir);
        }
        deployableArtifactBuilderMap.clear();
        if (wrappedListener != null) {
            wrappedListener.sessionEnded(event);
        }
    }

    @Override
    public void projectSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectSkipped(event);
        }
    }

    @Override
    public void projectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        initModule(project);

        if (wrappedListener != null) {
            wrappedListener.projectStarted(event);
        }
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        finalizeModule(event.getProject());
        if (wrappedListener != null) {
            wrappedListener.projectSucceeded(event);
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectFailed(event);
        }
    }

    @Override
    public void forkStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkStarted(event);
        }
    }

    @Override
    public void forkSucceeded(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkSucceeded(event);
        }
    }

    @Override
    public void forkFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkFailed(event);
        }
    }

    @Override
    public void forkedProjectStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectStarted(event);
        }
    }

    @Override
    public void forkedProjectSucceeded(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectSucceeded(event);
        }
    }

    @Override
    public void forkedProjectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectFailed(event);
        }
    }

    @Override
    public void mojoSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoSkipped(event);
        }
    }

    @Override
    public void mojoStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoStarted(event);
        }
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info dependency extraction: Null project.");
            return;
        }
        if ("maven-surefire-plugin".equals((event).getMojoExecution().getPlugin().getArtifactId())) {
            List<File> resultsFile = getSurefireResultsFile(project);
            if (isTestsFailed(resultsFile)) {
                projectHasTestFailures = true;
            }
        }

        extractModuleDependencies(project);

        if (wrappedListener != null) {
            wrappedListener.mojoSucceeded(event);
        }
    }

    private List<File> getSurefireResultsFile(MavenProject project) {
        List<File> surefireReports = Lists.newArrayList();
        File surefireDirectory = new File(new File(project.getFile().getParentFile(), "target"), "surefire-reports");
        String[] xmls;
        try {
            xmls = surefireDirectory.list(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith("xml");
                }
            });
        } catch (Exception e) {
            logger.error("Error occurred: " + e.getMessage() + " while retrieving surefire descriptors at: " +
                    surefireDirectory.getAbsolutePath(), e);
            return Lists.newArrayList();
        }
        if (xmls != null) {
            for (String xml : xmls) {
                surefireReports.add(new File(surefireDirectory, xml));
            }
        }
        return surefireReports;
    }

    private boolean isTestsFailed(List<File> surefireReports) {
        XPathFactory factory = XPathFactory.newInstance();
        XPath path = factory.newXPath();
        for (File report : surefireReports) {
            FileInputStream stream = null;
            try {
                stream = new FileInputStream(report);
                Object evaluate = path.evaluate("/testsuite/@failures", new InputSource(stream),
                        XPathConstants.STRING);
                if (evaluate != null && StringUtils.isNotBlank(evaluate.toString())
                        && StringUtils.isNumeric(evaluate.toString())) {
                    int testsFailed = Integer.parseInt(evaluate.toString());
                    return testsFailed != 0;
                }
            } catch (FileNotFoundException e) {
                logger.error("File '" + report.getAbsolutePath() + "' does not exist.");
            } catch (XPathExpressionException e) {
                logger.error("Expression /testsuite/@failures is invalid.");
            } finally {
                Closeables.closeQuietly(stream);
            }
        }
        return false;
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info dependency extraction: Null project.");
            return;
        }
        extractModuleDependencies(project);

        if (wrappedListener != null) {
            wrappedListener.mojoFailed(event);
        }
    }

    private void initModule(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }
        ModuleBuilder module = new ModuleBuilder();
        module.id(getModuleIdString(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        module.properties(project.getProperties());

        currentModule.set(module);

        currentModuleArtifacts.set(Sets.<Artifact>newHashSet());
        currentModuleDependencies.set(Sets.<Artifact>newHashSet());
    }

    private void extractArtifactsAndDependencies(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info artifact and dependency extraction: Null project.");
            return;
        }
        Set<Artifact> artifacts = currentModuleArtifacts.get();
        if (artifacts == null) {
            logger.warn("Skipping Artifactory Build-Info project artifact extraction: Null current module artifacts.");
        } else {
            extractModuleArtifact(project, artifacts);
            extractModuleAttachedArtifacts(project, artifacts);
        }

        extractModuleDependencies(project);
    }

    private void finalizeModule(MavenProject project) {
        extractArtifactsAndDependencies(project);
        finalizeAndAddModule(project);
    }

    private void extractModuleArtifact(MavenProject project, Set<Artifact> artifacts) {
        Artifact artifact = project.getArtifact();
        if (artifact == null) {
            logger.warn("Skipping Artifactory Build-Info project artifact extraction: Null artifact.");
            return;
        }
        artifacts.add(artifact);
    }

    private void extractModuleAttachedArtifacts(MavenProject project, Set<Artifact> artifacts) {
        List<Artifact> attachedArtifacts = project.getAttachedArtifacts();
        if (attachedArtifacts != null) {
            for (Artifact attachedArtifact : attachedArtifacts) {
                artifacts.add(attachedArtifact);
            }
        }
    }

    private void extractModuleDependencies(MavenProject project) {
        Set<Artifact> moduleDependencies = currentModuleDependencies.get();
        if (moduleDependencies == null) {
            logger.warn(
                    "Skipping Artifactory Build-Info project dependency extraction: Null current module dependencies.");
            return;
        }
        Set<Artifact> projectDependencies = project.getArtifacts();
        if (projectDependencies != null) {
            for (Artifact projectDependency : projectDependencies) {
                moduleDependencies.add(projectDependency);
            }
        }
    }

    private void finalizeAndAddModule(MavenProject project) {
        addFilesToCurrentModule(project);

        currentModule.remove();

        currentModuleArtifacts.remove();
        currentModuleDependencies.remove();
    }

    private void addFilesToCurrentModule(MavenProject project) {
        ModuleBuilder module = currentModule.get();
        if (module == null) {
            logger.warn("Skipping Artifactory Build-Info module finalization: Null current module.");
            return;
        }
        addArtifactsToCurrentModule(project, module);
        addDependenciesToCurrentModule(module);

        buildInfoBuilder.addModule(module.build());
    }

    private void addArtifactsToCurrentModule(MavenProject project, ModuleBuilder module) {
        Set<Artifact> moduleArtifacts = currentModuleArtifacts.get();
        if (moduleArtifacts == null) {
            logger.warn("Skipping Artifactory Build-Info module artifact addition: Null current module artifact list.");
            return;
        }

        for (Artifact moduleArtifact : moduleArtifacts) {
            String artifactId = moduleArtifact.getArtifactId();
            String artifactVersion = moduleArtifact.getVersion();
            String artifactClassifier = moduleArtifact.getClassifier();
            String artifactExtension = moduleArtifact.getArtifactHandler().getExtension();
            String type = getTypeString(moduleArtifact.getType(), artifactClassifier, artifactExtension);

            String artifactName = getArtifactName(artifactId, artifactVersion, artifactClassifier, artifactExtension);

            ArtifactBuilder artifactBuilder = new ArtifactBuilder(artifactName).type(type);
            File artifactFile = moduleArtifact.getFile();
            // for pom projects take the file from the project if the artifact file is null
            if (artifactFile == null && isPomProject(moduleArtifact) && moduleArtifact.equals(project.getArtifact())) {
                artifactFile = project.getFile();   // project.getFile() returns the project pom file
            }
            org.jfrog.build.api.Artifact artifact = artifactBuilder.build();
            module.addArtifact(artifact);
            if (isPublishArtifacts(artifactFile)) {
                addDeployableArtifact(artifact, artifactFile, moduleArtifact.getGroupId(),
                        artifactId, artifactVersion, artifactClassifier, artifactExtension);
            }

            if (!isPomProject(moduleArtifact)) {
                for (ArtifactMetadata metadata : moduleArtifact.getMetadataList()) {
                    if (metadata instanceof ProjectArtifactMetadata) {
                        Model model = project.getModel();
                        File pomFile = null;
                        if (model != null) {
                            pomFile = model.getPomFile();
                        }
                        artifactBuilder.type("pom");
                        String pomFileName = StringUtils.removeEnd(artifactName, artifactExtension) + "pom";
                        artifactBuilder.name(pomFileName);
                        org.jfrog.build.api.Artifact pomArtifact = artifactBuilder.build();
                        module.addArtifact(pomArtifact);
                        if (isPublishArtifacts(pomFile)) {
                            addDeployableArtifact(pomArtifact, pomFile, moduleArtifact.getGroupId(),
                                    artifactId, artifactVersion,
                                    artifactClassifier, "pom");
                        }
                    }
                }
            }
        }
    }

    private boolean isPublishArtifacts(File fileToDeploy) {
        if (fileToDeploy == null || !fileToDeploy.isFile()) {
            return false;
        }
        if (!conf.publisher.isPublishArtifacts()) {
            return false;
        }
        return conf.publisher.isEvenUnstable() || !projectHasTestFailures;
    }

    private String getArtifactName(String artifactId, String version, String classifier, String fileExtension) {
        StringBuilder nameBuilder = new StringBuilder(artifactId).append("-").append(version);
        if (StringUtils.isNotBlank(classifier)) {
            nameBuilder.append("-").append(classifier);
        }

        return nameBuilder.append(".").append(fileExtension).toString();
    }

    private void addDeployableArtifact(org.jfrog.build.api.Artifact artifact, File artifactFile,
            String groupId, String artifactId, String version, String classifier, String fileExtension) {
        String deploymentPath = getDeploymentPath(groupId, artifactId, version, classifier, fileExtension);
        // deploy to snapshots or releases repository based on the deploy version
        String targetRepository = getTargetRepository(deploymentPath);

        DeployDetails deployable = new DeployDetails.Builder().artifactPath(deploymentPath).file(artifactFile).
                targetRepository(targetRepository).addProperties(conf.publisher.getMatrixParams()).build();
        String myArtifactId = BuildInfoExtractorUtils.getArtifactId(currentModule.get().build().getId(), artifact.getName());
        deployableArtifactBuilderMap.put(myArtifactId, deployable);
    }

    /**
     * @param deployPath the full path string to extract the repo from
     * @return Return the target deployment repository. Either the releases repository (default) or snapshots if defined
     *         and the deployed file is a snapshot.
     */
    public String getTargetRepository(String deployPath) {
        String snapshotsRepository = conf.publisher.getSnapshotRepoKey();
        if (snapshotsRepository != null && deployPath.contains("-SNAPSHOT")) {
            return snapshotsRepository;
        }
        return conf.publisher.getRepoKey();
    }

    private String getDeploymentPath(String groupId, String artifactId, String version, String classifier,
            String fileExtension) {
        return new StringBuilder(groupId.replace(".", "/")).append("/").append(artifactId).append("/").append(version).
                append("/").append(getArtifactName(artifactId, version, classifier, fileExtension)).toString();
    }

    private void addDependenciesToCurrentModule(ModuleBuilder module) {
        Set<Artifact> moduleDependencies = currentModuleDependencies.get();
        if (moduleDependencies == null) {
            logger.warn("Skipping Artifactory Build-Info module dependency addition: Null current module dependency " +
                    "list.");
            return;
        }
        for (Artifact dependency : moduleDependencies) {
            File depFile = dependency.getFile();
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(getModuleIdString(dependency.getGroupId(), dependency.getArtifactId(),
                            dependency.getVersion()))
                    .type(getTypeString(dependency.getType(),
                            dependency.getClassifier(), getExtension(depFile)));
            String scopes = dependency.getScope();
            if (StringUtils.isNotBlank(scopes)) {
                dependencyBuilder.scopes(Lists.newArrayList(scopes));
            }
            setDependencyChecksums(depFile, dependencyBuilder);
            module.addDependency(dependencyBuilder.build());
        }
    }

    private String getExtension(File depFile) {
        String extension = "";
        String fileName = depFile.getName();
        if (depFile != null && fileName != null) {
            int lastDot = fileName.lastIndexOf('.');
            if (lastDot > 0 && lastDot + 1 < fileName.length()) {
                extension = fileName.substring(lastDot + 1);
            }
        }
        return extension;
    }

    private boolean isPomProject(Artifact moduleArtifact) {
        return "pom".equals(moduleArtifact.getType());
    }

    private void setDependencyChecksums(File dependencyFile, DependencyBuilder dependencyBuilder) {
        if ((dependencyFile != null) && (dependencyFile.isFile())) {
            try {
                Map<String, String> checksumsMap =
                        FileChecksumCalculator.calculateChecksums(dependencyFile, "md5", "sha1");
                dependencyBuilder.md5(checksumsMap.get("md5"));
                dependencyBuilder.sha1(checksumsMap.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + dependencyBuilder.build().getId() + "': " +
                        e.getMessage(), e);
            }
        }
    }

    @Override
    public Build extract(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if (!session.getResult().hasExceptions()) {
            if (conf.isIncludeEnvVars()) {
                Properties envProperties = new Properties();
                envProperties.putAll(conf.getAllProperties());
                envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties);
                for (Map.Entry<Object, Object> envProp : envProperties.entrySet()) {
                    buildInfoBuilder.addProperty(envProp.getKey(), envProp.getValue());
                }
            }
            Date finish = new Date();
            long time = finish.getTime() - session.getRequest().getStartTime().getTime();

            return buildInfoBuilder.durationMillis(time).build();
        }

        return null;
    }
}
