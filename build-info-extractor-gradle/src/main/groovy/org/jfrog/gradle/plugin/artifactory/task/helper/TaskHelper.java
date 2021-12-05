package org.jfrog.gradle.plugin.artifactory.task.helper;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jfrog.build.extractor.clientConfiguration.ArtifactSpec;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPluginUtil;
import org.jfrog.gradle.plugin.artifactory.extractor.PublishArtifactInfo;
import org.jfrog.gradle.plugin.artifactory.task.ArtifactoryTask;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Lior Hasson
 */
public abstract class TaskHelper {
    private static final Logger log = Logging.getLogger(TaskHelper.class);

    protected ArtifactoryTask artifactoryTask;

    public TaskHelper(ArtifactoryTask artifactoryTask) {
        this.artifactoryTask = artifactoryTask;
    }

    protected Map<String, String> defaultProps;

    public Project getProject() {
        return artifactoryTask.getProject();
    }

    public Task dependsOn(final Object... paths) {
        return artifactoryTask.dependsOn(paths);
    }

    public String getPath() {
        return artifactoryTask.getPath();
    }

    /**
     * Collects the list of publications and configurations.
     *
     * @param objects - The publication/configuration
     */
    public abstract void addCollection(Object... objects);

    protected Map<String, String> getPropsToAdd(PublishArtifactInfo artifact, String publicationName) {
        Project project = getProject();
        if (defaultProps == null) {
            defaultProps = new HashMap<>();
            addProps(defaultProps, artifactoryTask.getProperties());
            // Add the publisher properties
            ArtifactoryClientConfiguration.PublisherHandler publisher =
                    ArtifactoryPluginUtil.getPublisherHandler(project);
            if (publisher != null) {
                defaultProps.putAll(publisher.getMatrixParams());
            }
        }

        Map<String, String> propsToAdd = new HashMap<>(defaultProps);
        //Apply artifact-specific props from the artifact specs
        ArtifactSpec spec =
                ArtifactSpec.builder().configuration(publicationName)
                        .group(project.getGroup().toString())
                        .name(project.getName()).version(project.getVersion().toString())
                        .classifier(artifact.getClassifier())
                        .type(artifact.getType()).build();
        Multimap<String, CharSequence> artifactSpecsProperties = artifactoryTask.artifactSpecs.getProperties(spec);
        addProps(propsToAdd, artifactSpecsProperties);
        return propsToAdd;
    }

    private void addProps(Map<String, String> target, Multimap<String, CharSequence> props) {
        for (Map.Entry<String, CharSequence> entry : props.entries()) {
            // Make sure all GString are now Java Strings
            String key = entry.getKey();
            String value = entry.getValue().toString();
            //Accumulate multi-value props
            if (!target.containsKey(key)) {
                target.put(key, value);
            } else {
                value = target.get(key) + ", " + value;
                target.put(key, value);
            }
        }
    }

    @Nonnull
    protected Boolean isPublishMaven() {
        ArtifactoryClientConfiguration.PublisherHandler publisher =
                ArtifactoryPluginUtil.getPublisherHandler(getProject());
        if (publisher == null) {
            return false;
        }
        // Get the value from the client publisher configuration (in case a CI plugin configuration is used):
        Boolean publishPom = publisher.isMaven();
        // It the value is null, it means that there's no CI plugin configuration, so the value should be taken from the
        // artifactory DSL inside the gradle script:
        if (publishPom == null) {
            publishPom = artifactoryTask.getPublishPom();
        }
        return publishPom != null ? publishPom : true;
    }

    @Nonnull
    protected Boolean isPublishIvy() {
        ArtifactoryClientConfiguration.PublisherHandler publisher =
                ArtifactoryPluginUtil.getPublisherHandler(getProject());
        if (publisher == null) {
            return false;
        }
        // Get the value from the client publisher configuration (in case a CI plugin configuration is used):
        Boolean publishIvy = publisher.isIvy();
        // It the value is null, it means that there's no CI Server Artifactory plugin configuration,
        // so the value should be taken from the artifactory DSL inside the gradle script:
        if (publishIvy == null) {
            publishIvy = artifactoryTask.getPublishIvy();
        }
        return publishIvy != null ? publishIvy : true;
    }

    /**
     * @param deployPath the full path string to deploy the artifact.
     * @return Target deployment repository.
     * If snapshot repository is defined and artifact's version is snapshot, deploy to snapshot repository.
     * Otherwise, return the corresponding release repository.
     */
    protected String getTargetRepository(String deployPath, ArtifactoryClientConfiguration.PublisherHandler publisher) {
        String snapshotsRepository = publisher.getSnapshotRepoKey();
        if (snapshotsRepository != null && deployPath.contains("-SNAPSHOT")) {
            return snapshotsRepository;
        }
        if (StringUtils.isNotEmpty(publisher.getReleaseRepoKey())) {
            return publisher.getReleaseRepoKey();
        }
        return publisher.getRepoKey();
    }
}

