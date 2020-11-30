package org.jfrog.build.extractor.packageManager;

import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import static org.jfrog.build.api.util.CommonUtils.handleJavaTmpdirProperty;

/**
 * Created by Bar Belity on 12/07/2020.
 */
public abstract class PackageManagerExtractor implements Serializable {

    private static final long serialVersionUID = 1L;

    public abstract Build execute();

    /**
     * Run build-tool command and save build info to file.
     *
     * @param clientConfiguration - The client configuration.
     * @throws RuntimeException in case of any error.
     */
    public void executeAndSaveBuildInfo(ArtifactoryClientConfiguration clientConfiguration) throws RuntimeException {
        handleJavaTmpdirProperty();
        Build build = execute();
        if (build == null) {
            return;
        }
        saveBuildInfoToFile(clientConfiguration, build);
    }

    /**
     * Save the calculated build info .
     *
     * @param clientConfiguration - The client configuration
     * @param build               - The build to save
     */
    static void saveBuildInfoToFile(ArtifactoryClientConfiguration clientConfiguration, Build build) {
        String generatedBuildInfoPath = clientConfiguration.info.getGeneratedBuildInfoFilePath();
        if (StringUtils.isBlank(generatedBuildInfoPath)) {
            return;
        }
        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(generatedBuildInfoPath));
        } catch (Exception e) {
            clientConfiguration.getLog().error("Failed writing build info to file: ", e);
            throw new RuntimeException(e);
        }
    }

    protected static void validateRepoExists(ArtifactoryBaseClient client, String repo, String repoNotSpecifiedMsg) throws IOException {
        if (StringUtils.isBlank(repo)) {
            throw new IllegalArgumentException(repoNotSpecifiedMsg);
        }
        if (!client.isRepoExist(repo)) {
            throw new IOException("Repo " + repo + " doesn't exist");
        }
    }
}
