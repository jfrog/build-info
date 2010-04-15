package org.jfrog.build.extractor.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.Upload;
import org.jfrog.build.api.Build;
import org.jfrog.build.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.jfrog.build.ArtifactoryPluginUtils.getProperty;
import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_EXPORT_FILE_PATH;

/**
 * @author Tomer Cohen
 */
public class BuildInfoRecorderTask extends ConventionTask {
    private static final Logger log = Logging.getLogger(BuildInfoRecorderTask.class);

    private Configuration configuration;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return The root project.
     */
    public Project getRootProject() {
        return getProject().getRootProject();
    }

    @TaskAction
    public void collectProjectBuildInfo() throws IOException {
        log.debug("Starting extraction for project {}", getProject());
        Project rootProject = getRootProject();
        if (getProject().equals(rootProject)) {
            GradleBuildInfoExtractor infoExtractor = new GradleBuildInfoExtractor(rootProject);
            closeAndDeploy(infoExtractor);
        }
    }

    /**
     * Returns the artifacts which will be uploaded.
     *
     * @param gbie Tomer will document later.
     * @throws java.io.IOException Tomer will document later.
     */

    private void closeAndDeploy(GradleBuildInfoExtractor gbie) throws IOException {
        String uploadArtifactsProperty = getProperty(ClientProperties.PROP_PUBLISH_ARTIFACT, getRootProject());
        String fileExportPath = getProperty(PROP_EXPORT_FILE_PATH, getRootProject());

        if (Boolean.parseBoolean(uploadArtifactsProperty)) {
            /**
             * if the {@link org.jfrog.build.client.ClientProperties.PROP_PUBLISH_ARTIFACT} is set the true,
             * The uploadArchives task will be triggered ONLY at the end, ensuring that the artifacts will be published
             * only after a successful build. This is done before the build-info is sent.
             */
            for (Project uploadingProject : getRootProject().getAllprojects()) {
                Set<Task> uploadTask = uploadingProject.getTasksByName("uploadArchives", false);
                if (uploadTask != null) {
                    for (Task task : uploadTask) {
                        try {
                            log.debug("Uploading project {}", uploadingProject);
                            ((Upload) task).execute();
                        } catch (Exception e) {
                            throw new GradleException("Unable to upload project: " + uploadingProject, e);
                        }

                    }
                }
            }
            /**
             * After all the artifacts were uploaded successfully the next task is to send the build-info
             * object.
             */
            String buildInfoUploadUrl = getProperty(ClientProperties.PROP_CONTEXT_URL, getRootProject());
            Build build = gbie.extract(this);
            if (fileExportPath != null) {
                // If export property set always save the file before sending it to artifactory
                exportBuildInfo(build, new File(fileExportPath));
            }
            ArtifactoryBuildInfoClient artifactoryBuildInfoClient =
                    new ArtifactoryBuildInfoClient(buildInfoUploadUrl);
            artifactoryBuildInfoClient.sendBuildInfo(build);
        } else {
            /**
             * If we do not deploy any artifacts or build-info, the build-info will be written to a file in its
             * JSON form.
             */
            File savedFile;
            if (fileExportPath == null) {
                savedFile = new File(getProject().getBuildDir(), "build-info.json");
            } else {
                savedFile = new File(fileExportPath);
            }
            Build build = gbie.extract(this);
            exportBuildInfo(build, savedFile);
        }
    }

    private void exportBuildInfo(Build build, File toFile) throws IOException {
        BuildInfoExtractorUtils.saveBuildInfoToFile(build, toFile);
    }
}
