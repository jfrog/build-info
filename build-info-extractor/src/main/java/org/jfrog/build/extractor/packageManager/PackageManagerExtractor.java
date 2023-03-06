package org.jfrog.build.extractor.packageManager;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static org.jfrog.build.api.util.CommonUtils.handleJavaTmpdirProperty;

/**
 * Created by Bar Belity on 12/07/2020.
 */
public abstract class PackageManagerExtractor implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract BuildInfo execute();

    /**
     * Run build-tool command and save build info to file.
     *
     * @param clientConfiguration - The client configuration.
     * @throws RuntimeException in case of any error.
     */
    public void executeAndSaveBuildInfo(ArtifactoryClientConfiguration clientConfiguration) throws RuntimeException {
        // During build extractor's job, temp directories are created. This will make sure 'java.io.tmpdir' property is defined in Unix.
        handleJavaTmpdirProperty();
        BuildInfo buildInfo = execute();
        if (buildInfo == null) {
            return;
        }
        PackageManagerUtils.collectAndFilterEnvIfNeeded(clientConfiguration, buildInfo);
        saveBuildInfoToFile(clientConfiguration, buildInfo);
    }

    /**
     * Save the calculated build info .
     *
     * @param clientConfiguration - The client configuration
     * @param buildInfo           - The build-info to save
     */
    static void saveBuildInfoToFile(ArtifactoryClientConfiguration clientConfiguration, BuildInfo buildInfo) {
        String generatedBuildInfoPath = clientConfiguration.info.getGeneratedBuildInfoFilePath();
        if (StringUtils.isBlank(generatedBuildInfoPath)) {
            return;
        }
        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(buildInfo, new File(generatedBuildInfoPath));
        } catch (Exception e) {
            clientConfiguration.getLog().error("Failed writing build info to file: ", e);
            throw new RuntimeException(e);
        }
    }

    protected static void validateRepoExists(ArtifactoryManager client, String repo, String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!client.isRepositoryExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }
}
