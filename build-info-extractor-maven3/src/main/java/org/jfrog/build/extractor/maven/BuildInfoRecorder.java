/*
 * Copyright (C) 2010 JFrog Ltd.
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
import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildAgent;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorSpec;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.BuildInfoProperties.*;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildInfoRecorder.class)
public class BuildInfoRecorder implements BuildInfoExtractor<ExecutionEvent, Build>, ExecutionListener {

    public static final String PREFIX = "org.jfrog.build.extractor.maven.";

    @Requirement
    private Logger logger;

    private ExecutionListener wrappedListener;
    private BuildInfoBuilder buildInfoBuilder;
    private ModuleBuilder currentModule;
    private Set<Artifact> currentModuleArtifacts;
    private Set<Artifact> currentModuleDependencies;
    private Set<DeployDetails> deployableArtifacts = null;

    public void setListenerToWrap(ExecutionListener executionListener) {
        wrappedListener = executionListener;
    }

    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectDiscoveryStarted(event);
        }
    }

    public void sessionStarted(ExecutionEvent event) {
        logger.info("Initializing Artifactory Build-Info Recording");
        initBuildInfo(event);
        deployableArtifacts = Sets.newHashSet();

        if (wrappedListener != null) {
            wrappedListener.sessionStarted(event);
        }
    }

    public void sessionEnded(ExecutionEvent event) {
        MavenSession session = event.getSession();
        BuildSummary summary = session.getResult().getBuildSummary(session.getTopLevelProject());
        if (summary instanceof BuildSuccess) {
            summary.getTime();
        }

        deployableArtifacts = null;
        if (wrappedListener != null) {
            wrappedListener.sessionEnded(event);
        }
    }

    public void projectSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectSkipped(event);
        }
    }

    public void projectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        initModule(project);

        if (wrappedListener != null) {
            wrappedListener.projectStarted(event);
        }
    }

    public void projectSucceeded(ExecutionEvent event) {
        finalizeModule(event.getProject());
        if (wrappedListener != null) {
            wrappedListener.projectSucceeded(event);
        }
    }

    public void projectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectFailed(event);
        }
    }

    public void forkStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkStarted(event);
        }
    }

    public void forkSucceeded(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkSucceeded(event);
        }
    }

    public void forkFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkFailed(event);
        }
    }

    public void forkedProjectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        initModule(project);
        if (wrappedListener != null) {
            wrappedListener.forkedProjectStarted(event);
        }
    }

    public void forkedProjectSucceeded(ExecutionEvent event) {
        finalizeModule(event.getProject());
        if (wrappedListener != null) {
            wrappedListener.forkedProjectSucceeded(event);
        }
    }

    public void forkedProjectFailed(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.forkedProjectFailed(event);
        }
    }

    public void mojoSkipped(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoSkipped(event);
        }
    }

    public void mojoStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.mojoStarted(event);
        }
    }

    public void mojoSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info dependency extraction: Null project.");
            return;
        }
        extractModuleDependencies(project);

        if (wrappedListener != null) {
            wrappedListener.mojoSucceeded(event);
        }
    }

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

    private void initBuildInfo(ExecutionEvent event) {
        MavenSession session = event.getSession();

        Properties systemProperties = session.getSystemProperties();

        Properties mavenProperties = new Properties();
        InputStream inputStream = BuildInfoRecorder.class.getClassLoader()
                .getResourceAsStream("org/apache/maven/messages/build.properties");
        try {
            mavenProperties.load(inputStream);
        } catch (IOException e) {
            IOUtils.closeQuietly(inputStream);
        }

        buildInfoBuilder = new BuildInfoBuilder(systemProperties.getProperty(PREFIX + PROP_BUILD_NAME)).
                number(systemProperties.getProperty(PREFIX + PROP_BUILD_NUMBER)).
                started(systemProperties.getProperty(PREFIX + PROP_BUILD_STARTED)).
                url(systemProperties.getProperty(PREFIX + PROP_BUILD_URL)).
                artifactoryPrincipal(systemProperties.getProperty(PREFIX + "deployer.username")).
                agent(new Agent(systemProperties.getProperty(PREFIX + "agent.name"),
                        systemProperties.getProperty(PREFIX + "agent.version"))).
                buildAgent(new BuildAgent("Maven", mavenProperties.getProperty("version"))).
                principal(systemProperties.getProperty(PREFIX + "triggeredBy")).
                vcsRevision(systemProperties.getProperty(PREFIX + PROP_VCS_REVISION)).
                parentName(systemProperties.getProperty(PREFIX + PROP_PARENT_BUILD_NAME)).
                parentNumber(systemProperties.getProperty(PREFIX + PROP_PARENT_BUILD_NUMBER)).
                properties(gatherBuildInfoProperties());
    }

    private void initModule(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }
        currentModule = new ModuleBuilder();
        currentModule.id(project.getId());
        currentModule.properties(project.getProperties());

        currentModuleArtifacts = Sets.newHashSet();
        currentModuleDependencies = Sets.newHashSet();
    }

    private void extractArtifactsAndDependencies(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info artifact and dependency extraction: Null project.");
            return;
        }
        extractModuleArtifact(project);
        extractModuleAttachedArtifacts(project);
        extractModuleDependencies(project);
    }

    private void finalizeModule(MavenProject project) {
        extractArtifactsAndDependencies(project);
        finalizeAndAddModule(project);
    }

    private void extractModuleArtifact(MavenProject project) {
        Artifact artifact = project.getArtifact();
        if (artifact == null) {
            logger.warn("Skipping Artifactory Build-Info project artifact extraction: Null artifact.");
            return;
        }
        currentModuleArtifacts.add(artifact);
    }

    private void extractModuleAttachedArtifacts(MavenProject project) {
        List<Artifact> artifacts = project.getAttachedArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                currentModuleArtifacts.add(artifact);
            }
        }
    }

    private void extractModuleDependencies(MavenProject project) {
        Set<Artifact> dependencies = project.getArtifacts();
        if (dependencies != null) {
            for (Artifact dependency : dependencies) {
                currentModuleDependencies.add(dependency);
            }
        }
    }

    private void finalizeAndAddModule(MavenProject project) {
        addFilesToCurrentModule(project);

        currentModule = null;

        currentModuleArtifacts = null;
        currentModuleDependencies = null;
    }

    private void addFilesToCurrentModule(MavenProject project) {
        if (currentModule == null) {
            logger.warn("Skipping Artifactory Build-Info module finalization: Null current module.");
            return;
        }
        addArtifactsToCurrentModule(project);
        addDependenciesToCurrentModule();

        buildInfoBuilder.addModule(currentModule.build());
    }

    private void addArtifactsToCurrentModule(MavenProject project) {
        if (currentModuleArtifacts == null) {
            logger.warn("Skipping Artifactory Build-Info module artifact addition: Null current module artifact list.");
            return;
        }

        for (Artifact moduleArtifact : currentModuleArtifacts) {
            ArtifactBuilder artifactBuilder = new ArtifactBuilder(moduleArtifact.getId())
                    .type(moduleArtifact.getType());
            setArtifactChecksums(moduleArtifact.getFile(), artifactBuilder);
            currentModule.addArtifact(artifactBuilder.build());

            if (!isPomProject(moduleArtifact)) {
                for (ArtifactMetadata metadata : moduleArtifact.getMetadataList()) {
                    if (metadata instanceof ProjectArtifactMetadata) {
                        Model model = project.getModel();
                        if (model != null) {
                            File pomFile = model.getPomFile();
                            setArtifactChecksums(pomFile, artifactBuilder);
                        }
                        artifactBuilder.type("pom");
                        artifactBuilder.name(moduleArtifact.getId().replace(moduleArtifact.getType(), "pom"));
                        currentModule.addArtifact(artifactBuilder.build());
                    }
                }
            }
        }
    }

    private void addDependenciesToCurrentModule() {
        if (currentModuleDependencies == null) {
            logger.warn("Skipping Artifactory Build-Info module dependency addition: Null current module dependency " +
                    "list.");
            return;
        }
        for (Artifact dependency : currentModuleDependencies) {
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(dependency.getId())
                    .type(dependency.getType())
                    .scopes(Lists.newArrayList(dependency.getScope()));
            setDependencyChecksums(dependency.getFile(), dependencyBuilder);
            currentModule.addDependency(dependencyBuilder.build());
        }
    }

    private boolean isPomProject(Artifact moduleArtifact) {
        return "pom".equals(moduleArtifact.getType());
    }

    private void setArtifactChecksums(File artifactFile, ArtifactBuilder artifactBuilder) {
        if ((artifactFile != null) && (artifactFile.isFile())) {
            try {
                Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "md5", "sha1");
                artifactBuilder.md5(checksums.get("md5"));
                artifactBuilder.sha1(checksums.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + artifactBuilder.build().getName() + "': " +
                        e.getMessage());
            }
        }
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
                        e.getMessage());
            }
        }
    }

    public Build extract(ExecutionEvent event, BuildInfoExtractorSpec spec) {
        //TODO: [by yl] Handle the spec!!!

        long startTime = event.getSession().getStartTime().getTime();
        return buildInfoBuilder.durationMillis(startTime).build();
    }

    protected Properties gatherBuildInfoProperties() {
        Properties props = new Properties();
        props.setProperty("os.arch", System.getProperty("os.arch"));
        props.setProperty("os.name", System.getProperty("os.name"));
        props.setProperty("os.version", System.getProperty("os.version"));
        props.setProperty("java.version", System.getProperty("java.version"));
        props.setProperty("java.vm.info", System.getProperty("java.vm.info"));
        props.setProperty("java.vm.name", System.getProperty("java.vm.name"));
        props.setProperty("java.vm.specification.name", System.getProperty("java.vm.specification.name"));
        props.setProperty("java.vm.vendor", System.getProperty("java.vm.vendor"));

        return props;
    }
}