package org.jfrog.gradle.plugin.artifactory.task;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import groovy.lang.Closure;
import org.apache.commons.lang.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.util.ConfigureUtil;
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpecs;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.dsl.PropertiesConfig;
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig;
import org.jfrog.gradle.plugin.artifactory.extractor.GradleDeployDetails;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Date: 3/20/13 Time: 10:32 AM
 *
 * @author freds
 */
public abstract class BuildInfoBaseTask extends DefaultTask {
    public static final String DEPLOY_TASK_NAME = "artifactoryDeploy";
    public static final String BUILD_INFO_TASK_NAME = "artifactoryPublish";
    public static final String PUBLISH_ARTIFACTS = "publishArtifacts";
    public static final String PUBLISH_IVY = "publishIvy";
    public static final String PUBLISH_POM = "publishPom";

    private static final Logger log = Logging.getLogger(BuildInfoBaseTask.class);
    private final Map<String, Boolean> flags = Maps.newHashMap();
    private boolean evaluated = false;

    public final Set<GradleDeployDetails> deployDetails = Sets.newTreeSet();

    public abstract void checkDependsOnArtifactsToPublish();

    public abstract void collectDescriptorsAndArtifactsForUpload() throws IOException;

    public abstract boolean hasModules();

    private final Multimap<String, CharSequence> properties = ArrayListMultimap.create();

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



    public void projectsEvaluated() {
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

                // Configure the task using the "defaults" closure (delegate to the task)
                PublisherConfig config = convention.getPublisherConfig();
                if (config != null) {
                    Closure defaultsClosure = config.getDefaultsClosure();
                    ConfigureUtil.configure(defaultsClosure, this);
                }
            }

            // Depend on buildInfo task in sub-projects
            Task deployTask = project.getRootProject().getTasks().findByName(DEPLOY_TASK_NAME);
            finalizedBy(deployTask);

            for (Project sub : project.getSubprojects()) {
                Task subBiTask = sub.getTasks().findByName(BUILD_INFO_TASK_NAME);
                if (subBiTask != null) {
                    dependsOn(subBiTask);
                }
            }

            checkDependsOnArtifactsToPublish();
        }
        evaluated = true;
    }

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

    public Set<GradleDeployDetails> getDeployDetails() {
        return deployDetails;
    }

    //For testing
    public ArtifactSpecs getArtifactSpecs() {
        return artifactSpecs;
    }

    /**
     * Setters (Object and DSL)
     **/

    public void properties(Closure closure) {
        Project project = getProject();
        PropertiesConfig propertiesConfig = new PropertiesConfig(project);
        ConfigureUtil.configure(closure, propertiesConfig);
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

    //Publish artifacts to Artifactory (true by default)
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
