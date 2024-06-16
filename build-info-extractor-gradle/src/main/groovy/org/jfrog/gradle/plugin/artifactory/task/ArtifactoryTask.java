package org.jfrog.gradle.plugin.artifactory.task;

import groovy.lang.Closure;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.*;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.api.multiMap.ListMultimap;
import org.jfrog.build.api.multiMap.Multimap;
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpecs;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.PropertiesConfig;
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;
import org.jfrog.gradle.plugin.artifactory.task.helper.TaskHelperConfigurations;
import org.jfrog.gradle.plugin.artifactory.task.helper.TaskHelperPublications;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author Lior Hasson
 */
public class ArtifactoryTask extends DefaultTask {
    public static final String DEPLOY_TASK_NAME = "artifactoryDeploy";
    public static final String ARTIFACTORY_PUBLISH_TASK_NAME = "artifactoryPublish";
    public static final String EXTRACT_MODULE_TASK_NAME = "extractModuleInfo";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";
    private boolean evaluated = false;

    private static final Logger log = Logging.getLogger(ArtifactoryTask.class);
    private final Map<String, Boolean> flags = new HashMap<>();

    public File ivyDescriptor;

    public File mavenDescriptor;

    public Set<Configuration> publishConfigs = new HashSet<>();

    public Set<IvyPublication> ivyPublications = new HashSet<>();

    public Set<MavenPublication> mavenPublications = new HashSet<>();

    private boolean ciServerBuild = false;
    public TaskHelperConfigurations helperConfigurations = new TaskHelperConfigurations(this);
    public TaskHelperPublications helperPublications = new TaskHelperPublications(this);

    @TaskAction
    public void taskAction() throws IOException {
        log.debug("Task '{}' activated", getPath());
    }

    private void checkDependsOnArtifactsToPublish() {
        helperConfigurations.checkDependsOnArtifactsToPublish();
        helperPublications.checkDependsOnArtifactsToPublish();
    }

    public void collectDescriptorsAndArtifactsForUpload() throws IOException {
        if (helperConfigurations.hasConfigurations()) {
            helperConfigurations.collectDescriptorsAndArtifactsForUpload();
        }
        if (helperPublications.hasPublications()) {
            helperPublications.collectDescriptorsAndArtifactsForUpload();
        }
    }

    public boolean hasModules() {
        return helperConfigurations.hasModules() || helperPublications.hasModules();
    }

    public boolean hasPublications() {
        return helperPublications.hasPublications();
    }

    public boolean hasConfigurations() {
        return helperConfigurations.hasConfigurations();
    }

    /**
     * DSL
     **/

    public void publishConfigs(Object... confs) {
        if (confs != null) {
            helperConfigurations.addCollection(confs);
            checkDependsOnArtifactsToPublish();
        }
    }

    public void publications(Object... publications) {
        if (publications != null) {
            helperPublications.addCollection(publications);
            checkDependsOnArtifactsToPublish();
        }
    }

    /**
     * Getters
     **/
    @Input
    Set<Publication> getPublications() {
        Set<Publication> publications = new HashSet<Publication>();
        publications.addAll(ivyPublications);
        publications.addAll(mavenPublications);
        return publications;
    }

    @InputFiles
    @Optional
    public Set<Configuration> getPublishConfigs() {
        return publishConfigs;
    }

    @Input
    @Optional
    public Set<IvyPublication> getIvyPublications() {
        return ivyPublications;
    }

    @Input
    @Optional
    public Set<MavenPublication> getMavenPublications() {
        return mavenPublications;
    }

    @InputFile
    @Optional
    public File getIvyDescriptor() {
        return ivyDescriptor;
    }

    @InputFile
    @Optional
    public File getMavenDescriptor() {
        return mavenDescriptor;
    }

    @Input
    public boolean isCiServerBuild() {
        return this.ciServerBuild;
    }

    public void setCiServerBuild() {
        this.ciServerBuild = true;
    }

    public final Set<GradleDeployDetails> deployDetails = new TreeSet<>();

    private final Multimap<String, CharSequence> properties = new ListMultimap<>();

    @Input
    public Multimap<String, CharSequence> getProperties() {
        return properties;
    }

    @Input
    public final ArtifactSpecs artifactSpecs = new ArtifactSpecs();

    @Input
    public boolean skip = false;

    @Input
    @Optional
    @Nullable
    public Boolean getPublishArtifacts() {
        return getFlag(PUBLISH_ARTIFACTS);
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishIvy() {
        return getFlag(PUBLISH_IVY);
    }

    @Input
    @Optional
    @Nullable
    public Boolean getPublishPom() {
        return getFlag(PUBLISH_POM);
    }

    public void projectEvaluated() {
        Project project = getProject();
        if (isSkip()) {
            log.debug("artifactoryPublish task '{}' skipped for project '{}'.",
                    this.getPath(), project.getName());
        } else {
            ArtifactoryPluginConvention convention = ArtifactoryPluginUtil.getPublisherConvention(project);
            if (convention != null) {
                ArtifactoryClientConfiguration acc = convention.getClientConfig();
                artifactSpecs.clear();
                artifactSpecs.addAll(acc.publisher.getArtifactSpecs());

                // Configure the task using the "defaults" action (delegate to the task)
                PublisherConfig config = convention.getPublisherConfig();
                if (config != null) {
                    Action<ArtifactoryTask> defaultsAction = config.getDefaultsAction();
                    if (defaultsAction != null) {
                        defaultsAction.execute(this);
                    }
                }
            }

            // Depend on buildInfo task in sub-projects
            for (Project sub : project.getSubprojects()) {
                Task subArtifactoryTask = sub.getTasks().findByName(ARTIFACTORY_PUBLISH_TASK_NAME);
                if (subArtifactoryTask != null) {
                    dependsOn(subArtifactoryTask);
                }
            }
        }
        evaluated = true;
    }


    @Deprecated
    public void addDefaultArchiveConfiguration() {
        helperConfigurations.addDefaultArchiveConfiguration();
    }

    public void addDefaultPublications() {
        helperPublications.addDefaultPublications();
    }

    /**
     * Sets the deploy task as a final task for this artifactoryPublish task
     *
     * @param project - The project itself
     */
    public void finalizeByDeployTask(Project project) {
        Task deployTask = project.getRootProject().getTasks().findByName(DEPLOY_TASK_NAME);
        if (deployTask == null) {
            throw new IllegalStateException(String.format("Could not find %s in the root project", DEPLOY_TASK_NAME));
        }
        finalizedBy(deployTask);
    }

    @Internal
    public boolean isEvaluated() {
        return evaluated;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setProperties(Map<String, CharSequence> props) {
        if (props == null || props.isEmpty()) {
            return;
        }
        properties.clear();
        for (Map.Entry<String, CharSequence> entry : props.entrySet()) {
            // The key cannot be lazy eval, but we keep the value as GString as long as possible
            String key = entry.getKey();
            if (StringUtils.isNotBlank(key)) {
                CharSequence value = entry.getValue();
                if (value != null) {
                    // Make sure all GString are now Java Strings for key,
                    // and don't call toString for value (keep lazy eval as long as possible)
                    // So, don't use HashMultimap this will call equals on the GString
                    this.properties.put(key, value);
                }
            }
        }
    }

    @Input
    @Optional
    public Set<GradleDeployDetails> getDeployDetails() {
        return deployDetails;
    }

    //For testing
    public ArtifactSpecs getArtifactSpecs() {
        return artifactSpecs;
    }

    public void properties(Closure closure) {
        properties(ConfigureUtil.configureUsing(closure));
    }

    public void properties(Action<PropertiesConfig> propertiesAction) {
        Project project = getProject();
        PropertiesConfig propertiesConfig = new PropertiesConfig(project);
        propertiesAction.execute(propertiesConfig);
        artifactSpecs.clear();
        artifactSpecs.addAll(propertiesConfig.getArtifactSpecs());
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public void setPublishIvy(Object publishIvy) {
        setFlag(PUBLISH_IVY, toBoolean(publishIvy));
    }

    public void setPublishPom(Object publishPom) {
        setFlag(PUBLISH_POM, toBoolean(publishPom));
    }

    // Publish artifacts to Artifactory (true by default)
    public void setPublishArtifacts(Object publishArtifacts) {
        setFlag(PUBLISH_ARTIFACTS, toBoolean(publishArtifacts));
    }

    @Nullable
    private Boolean getFlag(String flagName) {
        return flags.get(flagName);
    }

    private Boolean toBoolean(Object o) {
        return Boolean.valueOf(o.toString());
    }

    private void setFlag(String flagName, Boolean newValue) {
        flags.put(flagName, newValue);
    }
}
