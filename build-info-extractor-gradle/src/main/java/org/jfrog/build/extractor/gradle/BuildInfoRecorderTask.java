package org.jfrog.build.extractor.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GUtil;
import org.jfrog.build.api.Agent;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildType;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;
import java.util.Set;

import static org.jfrog.build.api.constants.BuildInfoProperties.*;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends ConventionTask implements BuildInfoExtractor<Project, Module> {
    private static final Logger log = Logging.getLogger(BuildInfoRecorderTask.class);

    private Configuration configuration;
    private Module module;


    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }


    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        log.debug("Starting extraction for project {}", getProject());
        module = extract(getProject());
        if (getProject().equals(getProject().getRootProject())) {
            closeAndDeploy(getProject());
        }
    }

    public Module extract(Project context) {
        return new BuildInfoRecorder(getConfiguration()).extract(context);
    }

    /**
     * Returns the artifacts which will be uploaded.
     *
     * @param project Tomer will document later.
     * @throws java.io.IOException Tomer will document later.
     */

    private void closeAndDeploy(Project project) throws IOException {
        Properties gradleProps = BuildInfoExtractorUtils.getBuildInfoProperties();
        File projectPropsFile = new File(project.getProjectDir(), Project.GRADLE_PROPERTIES);
        if (projectPropsFile.exists()) {
            Properties properties = GUtil.loadProperties(projectPropsFile);
            gradleProps.putAll(BuildInfoExtractorUtils.filterProperties(properties));
        }
        long startTime = Long.parseLong(System.getProperty("build.start"));
        String buildName = gradleProps.getProperty(PROP_BUILD_NAME);
        if (buildName == null) {
            buildName = project.getName();
        }
        BuildInfoBuilder buildInfoBuilder = new BuildInfoBuilder(buildName);
        Date startedDate = new Date();
        startedDate.setTime(startTime);
        buildInfoBuilder.type(BuildType.GRADLE);
        String buildNumber = gradleProps.getProperty(PROP_BUILD_NUMBER);
        if (buildNumber == null) {
            String message = "Build number not set, please provide system variable \'" + PROP_BUILD_NUMBER + "\'";
            log.error(message);
            throw new GradleException(message);
        }
        GradleInternal gradleInternals = (GradleInternal) project.getGradle();
        buildInfoBuilder.agent(new Agent("Gradle", gradleInternals.getGradleVersion()))
                .durationMillis(System.currentTimeMillis() - startTime)
                .startedDate(startedDate)
                .number(Long.parseLong(buildNumber));
        for (Project subProject : project.getSubprojects()) {
            addModule(buildInfoBuilder, subProject);
        }
        addModule(buildInfoBuilder, project);
        String parentName = gradleProps.getProperty(PROP_PARENT_BUILD_NAME);
        String parentNumber = gradleProps.getProperty(PROP_PARENT_BUILD_NUMBER);
        if (parentName != null && parentNumber != null) {
            String parent = parentName + ":" + parentNumber;
            buildInfoBuilder.parentBuildId(parent);
        }
        buildInfoBuilder.properties(gradleProps);
        Build build = buildInfoBuilder.build();
        log.debug("buildInfoBuilder = " + buildInfoBuilder);

        String fileExportPath = gradleProps.getProperty(PROP_EXPORT_FILE_PATH);
        if (fileExportPath == null) {
            throw new GradleException("Cannot have a null path to export the build-info");
        }
        File savedFile = new File(fileExportPath);
        OutputStream fileOutputStream = new FileOutputStream(savedFile);
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, fileOutputStream);
    }

    private static void addModule(BuildInfoBuilder buildInfoBuilder, Project project) {
        Set<Task> buildInfoTasks = project.getTasksByName("buildInfo", false);
        for (Task task : buildInfoTasks) {
            BuildInfoRecorderTask buildInfoTask = (BuildInfoRecorderTask) task;
            Module module = buildInfoTask.module;
            if (module != null) {
                buildInfoBuilder.addModule(module);
            }
        }
    }
}
