package org.jfrog.maven;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.artifactory.build.api.Agent;
import org.artifactory.build.api.BuildType;
import org.artifactory.build.api.builder.ArtifactBuilder;
import org.artifactory.build.api.builder.BuildInfoBuilder;
import org.artifactory.build.api.builder.DependencyBuilder;
import org.artifactory.build.api.builder.ModuleBuilder;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
public class ArtifactoryExecutionListener extends AbstractExecutionListener {

    private Logger logger;
    private ExecutionListener wrappedListener;
    private BuildInfoBuilder buildInfoBuilder;
    private ModuleBuilder currentModule;
    private Set<Artifact> currentModuleArtifacts;
    private Set<Artifact> currentModuleDependencies;

    public ArtifactoryExecutionListener(Logger logger, ExecutionListener listenerToWrap) {
        this.logger = logger;
        this.wrappedListener = listenerToWrap;
    }

    @Override
    public void projectDiscoveryStarted(ExecutionEvent event) {
        if (wrappedListener != null) {
            wrappedListener.projectDiscoveryStarted(event);
        }
    }

    @Override
    public void sessionStarted(ExecutionEvent event) {
        initBuildInfo(event);

        if (wrappedListener != null) {
            wrappedListener.sessionStarted(event);
        }
    }

    @Override
    public void sessionEnded(ExecutionEvent event) {
        addBuildDuration(event);

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
        extractArtifactsAndDependencies(event);
        finalizeAndAddModule(event);

        if (wrappedListener != null) {
            wrappedListener.projectSucceeded(event);
        }
    }

    @Override
    public void projectFailed(ExecutionEvent event) {
        extractArtifactsAndDependencies(event);
        finalizeAndAddModule(event);

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
        extractArtifactsAndDependencies(event);

        if (wrappedListener != null) {
            wrappedListener.mojoSucceeded(event);
        }
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        extractArtifactsAndDependencies(event);

        if (wrappedListener != null) {
            wrappedListener.mojoFailed(event);
        }
    }

    private void initBuildInfo(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if (session == null) {
            logger.warn("Skipping Artifactory Build-Info initialization: Null session.");
            return;
        }
        MavenProject project = session.getTopLevelProject();

        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info initialization: Null project.");
            return;
        }

        logger.info("Initializing Artifactory Build-Info");
        this.buildInfoBuilder = new BuildInfoBuilder(project.getName());

        BuildType buildType = BuildType.MAVEN;
        buildInfoBuilder.type(buildType);
        buildInfoBuilder.startedDate(session.getRequest().getStartTime());
        buildInfoBuilder.agent(new Agent(buildType.getName(), getMavenVersion()));
        buildInfoBuilder.version(project.getVersion());

        setProjectUrl(project);

        setParentProjectName(project);

        gatherSysPropInfo();
    }

    private void setProjectUrl(MavenProject project) {
        File projectDir = project.getBasedir();
        if (projectDir == null) {
            logger.warn("Could not set Artifactory Build-Info URL field: Null project base directory.");
            return;
        }
        buildInfoBuilder.url(projectDir.getAbsolutePath());
    }

    private void setParentProjectName(MavenProject project) {
        MavenProject projectParent = project.getParent();
        if (projectParent == null) {
            logger.warn("Could not set Artifactory Build-Info parent project name field: Null parent project.");
            return;
        }
        buildInfoBuilder.parentBuildId(projectParent.getName());
    }

    private void initModule(MavenProject project) {
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }
        currentModule = new ModuleBuilder();
        currentModule.id(project.getId());
        currentModule.properties(project.getProperties());

        currentModuleArtifacts = new HashSet<Artifact>();
        currentModuleDependencies = new HashSet<Artifact>();
    }

    private String getMavenVersion() {
        Properties properties = new Properties();
        InputStream resourceAsStream = null;
        try {
            resourceAsStream = Thread.currentThread().getContextClassLoader().
                    getResourceAsStream("org/apache/maven/messages/build.properties");

            if (resourceAsStream != null) {
                properties.load(resourceAsStream);
            }
        }
        catch (IOException e) {
            logger.error("Unable determine version from JAR file for Artifactory Build-Info: " + e.getMessage());
        }
        finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    System.err.println("Unable to close properties resource stream: " + e.getMessage());
                }
            }
        }

        String version = properties.getProperty("version");
        if (StringUtils.isBlank(version) || (version.startsWith("${") && version.endsWith("}"))) {
            return "N/A";
        }

        return version;
    }

    private void gatherSysPropInfo() {
        if (buildInfoBuilder == null) {
            logger.warn("Skipping Artifactory Build-Info system property collection: Null info builder.");
            return;
        }
        buildInfoBuilder.addProperty("os.arch", System.getProperty("os.arch"));
        buildInfoBuilder.addProperty("os.name", System.getProperty("os.name"));
        buildInfoBuilder.addProperty("os.version", System.getProperty("os.version"));
        buildInfoBuilder.addProperty("java.version", System.getProperty("java.version"));
        buildInfoBuilder.addProperty("java.vm.info", System.getProperty("java.vm.info"));
        buildInfoBuilder.addProperty("java.vm.name", System.getProperty("java.vm.name"));
        buildInfoBuilder
                .addProperty("java.vm.specification.name", System.getProperty("java.vm.specification.name"));
        buildInfoBuilder.addProperty("java.vm.vendor", System.getProperty("java.vm.vendor"));

    }

    private void addBuildDuration(ExecutionEvent event) {
        if (buildInfoBuilder == null) {
            logger.warn("Skipping build duration determination for Artifactory Build-Info: Null info builder.");
            return;
        }
        MavenSession session = event.getSession();

        if (session == null) {
            logger.warn("Skipping build duration determination for Artifactory Build-Info: Null session.");
            return;
        }
        MavenExecutionRequest request = session.getRequest();

        if (request == null) {
            logger.warn("Skipping build duration determination for Artifactory Build-Info: Null request.");
            return;
        }
        Date startTime = request.getStartTime();

        if (startTime == null) {
            logger.warn("Skipping build duration determination for Artifactory Build-Info: Null start time.");
            return;
        }
        Date finish = new Date();

        long time = finish.getTime() - startTime.getTime();

        buildInfoBuilder.durationMillis(time);

    }

    private void extractArtifactsAndDependencies(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info artifact and dependency extraction: Null project.");
            return;
        }
        extractModuleArtifact(project);
        extractModuleAttachedArtifacts(project);
        extractModuleDependencies(project);
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
        if (artifacts == null) {
            logger.warn("Skipping Artifactory Build-Info project attached artifact extraction: Null project attached " +
                    "artifacts list.");
            return;
        }
        for (Artifact artifact : artifacts) {
            currentModuleArtifacts.add(artifact);
        }
    }

    private void extractModuleDependencies(MavenProject project) {
        Set<Artifact> dependencies = project.getArtifacts();
        if (dependencies == null) {
            logger.warn("Skipping Artifactory Build-Info project dependency extraction: Null project artifacts list.");
            return;
        }
        for (Artifact dependency : dependencies) {
            currentModuleDependencies.add(dependency);
        }
    }

    private void finalizeAndAddModule(ExecutionEvent event) {
        addFilesToCurrentModule(event);

        currentModule = null;

        currentModuleArtifacts = null;
        currentModuleDependencies = null;
    }

    private void addFilesToCurrentModule(ExecutionEvent event) {
        if (currentModule == null) {
            logger.warn("Skipping Artifactory Build-Info module finalization: Null current module.");
            return;
        }
        addArtifactsToCurrentModule(event);
        addDependenciesToCurrentModule();

        buildInfoBuilder.addModule(currentModule.build());
    }

    private void addArtifactsToCurrentModule(ExecutionEvent event) {
        if (currentModuleArtifacts == null) {
            logger.warn("Skipping Artifactory Build-Info module artifact addition: Null current module artifact list.");
            return;
        }
        ArtifactRepository localRepository = getLocalRepository(event);
        boolean validLocalRepo = localRepository != null;

        for (Artifact moduleArtifact : currentModuleArtifacts) {
            ArtifactBuilder artifactBuilder = new ArtifactBuilder(moduleArtifact.getId())
                    .type(moduleArtifact.getType());
            currentModule.addArtifact(artifactBuilder.build());

            if (!"pom".equals(moduleArtifact.getType())) {
                for (ArtifactMetadata metadata : moduleArtifact.getMetadataList()) {
                    if (metadata instanceof ProjectArtifactMetadata) {
                        if (validLocalRepo) {
                            File file = new File(localRepository.getBasedir(), localRepository.
                                    pathOfLocalRepositoryMetadata((ProjectArtifactMetadata) metadata, localRepository));
                        }
                        artifactBuilder.type("pom");
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
        for (Artifact moduleDependency : currentModuleDependencies) {
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(moduleDependency.getId())
                    .type(moduleDependency.getType())
                    .scopes(Lists.newArrayList(moduleDependency.getScope()));
            currentModule.addDependency(dependencyBuilder.build());
        }
    }

    private ArtifactRepository getLocalRepository(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if (session == null) {
            logger.warn("Skipping local repository duration determination for Artifactory Build-Info: Null session.");
            return null;
        }
        return session.getLocalRepository();
    }
}
