package org.jfrog.gradle.plugin.artifactory.task;

import com.google.common.collect.Sets;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.publish.Publication;
import org.gradle.api.publish.ivy.IvyPublication;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.jfrog.gradle.plugin.artifactory.task.helper.TaskHelperConfigurations;
import org.jfrog.gradle.plugin.artifactory.task.helper.TaskHelperPublications;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Lior Hasson
 */
public class ArtifactoryTask extends BuildInfoBaseTask{
    @InputFile
    @Optional
    public File ivyDescriptor;

    @InputFile
    @Optional
    public File mavenDescriptor;

    @InputFiles
    @Optional
    public Set<Configuration> publishConfigs = Sets.newHashSet();

    @Input
    @Optional
    public Set<IvyPublication> ivyPublications = Sets.newHashSet();

    @Input
    @Optional
    public Set<MavenPublication> mavenPublications = Sets.newHashSet();

    private boolean addArchivesConfigToTask = false;
    public TaskHelperConfigurations helperConfigurations = new TaskHelperConfigurations(this);
    public TaskHelperPublications helperPublications = new TaskHelperPublications(this);

    @Input
    Set<Publication> getPublications() {
        Set<Publication> publications = new HashSet<Publication>();
        publications.addAll(ivyPublications);
        publications.addAll(mavenPublications);
        return publications;
    }

    @Override
    public void checkDependsOnArtifactsToPublish() {
        if (addArchivesConfigToTask) {
            helperConfigurations.AddDefaultArchiveConfiguration(getProject());
        }
        if (helperConfigurations.hasConfigurations()) {
            helperConfigurations.checkDependsOnArtifactsToPublish();
        }
        if (helperPublications.hasPublications()) {
            helperPublications.checkDependsOnArtifactsToPublish();
        }
    }

    @Override
    public void collectDescriptorsAndArtifactsForUpload() throws IOException {
        if(helperConfigurations.hasConfigurations()){
            helperConfigurations.collectDescriptorsAndArtifactsForUpload();
        }
        if(helperPublications.hasPublications()){
            helperPublications.collectDescriptorsAndArtifactsForUpload();
        }
    }

    @Override
    public boolean hasModules() {
        return helperConfigurations.hasModules() || helperPublications.hasModules();
    }

    public boolean hasPublications() {
        return helperPublications.hasPublications();
    }

    public boolean hasConfigurations() {
        return helperConfigurations.hasConfigurations();
    }

    /** DSL **/

    public void publishConfigs(Object... confs) {
        helperConfigurations.publishConfigs(confs);
    }

    public void publications(Object... publications) {
        helperPublications.publications(publications);
    }

    /** Getters **/

    public Set<IvyPublication> getIvyPublications() {
        return ivyPublications;
    }

    public File getIvyDescriptor() {
        return ivyDescriptor;
    }

    public File getMavenDescriptor() {
        return mavenDescriptor;
    }

    public void setAddArchivesConfigToTask(boolean addArchivesConfigToTask) {
        this.addArchivesConfigToTask = addArchivesConfigToTask;
    }
}
