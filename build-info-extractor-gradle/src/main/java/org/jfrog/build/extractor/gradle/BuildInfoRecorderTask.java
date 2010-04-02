package org.jfrog.build.extractor.gradle;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.TaskAction;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends ConventionTask implements BuildInfoExtractor<Project, Build> {

    private static final Logger log = LoggerFactory.getLogger(BuildInfoRecorderTask.class);

    private Configuration configuration;


    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


    @TaskAction
    public Build extract(Project context) {
        log.debug("Starting extraction for project {}", context);
        return new BuildInfoRecorder(getConfiguration()).extract(context);
    }
}
