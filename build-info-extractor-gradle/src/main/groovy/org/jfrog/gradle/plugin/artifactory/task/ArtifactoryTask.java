package org.jfrog.gradle.plugin.artifactory.task;

import com.google.common.collect.Sets;
import groovy.lang.Lazy;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
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
import java.util.Set;

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
    public Set<Configuration> publishConfigurations = Sets.newHashSet();

    @Input
    @Optional
    public Set<IvyPublication> ivyPublications = Sets.newHashSet();

    @Input
    @Optional
    public Set<MavenPublication> mavenPublications = Sets.newHashSet();

    public TaskHelperConfigurations helperConfigurations = new TaskHelperConfigurations(this);
    public TaskHelperPublications helperPublications = new TaskHelperPublications(this);

    @Override
    public void checkDependsOnArtifactsToPublish() {
        if(helperConfigurations.hasConfigurations())
            helperConfigurations.checkDependsOnArtifactsToPublish();
        if(helperPublications.hasPublications())
            helperPublications.checkDependsOnArtifactsToPublish();

        /*Backward compatibility for Gradle publish configuration approach:
        * Users that didn`t defined any configurations to the plugin, got the "archives" configuration
        * as default.
        */
        /*if(!hasConfigurations() && !hasPublications()){
            helperConfigurations.AddDefaultArchiveConfiguration(getProject());
            helperConfigurations.checkDependsOnArtifactsToPublish();
        }*/
    }

    @Override
    public void collectDescriptorsAndArtifactsForUpload() throws IOException {
        if(helperConfigurations.hasConfigurations())
            helperConfigurations.collectDescriptorsAndArtifactsForUpload();
        if(helperPublications.hasPublications())
            helperPublications.collectDescriptorsAndArtifactsForUpload();
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
}
