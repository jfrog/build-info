package org.jfrog.build.extractor.npm.types;

import java.nio.file.Path;
import java.util.List;

/**
 * Contains a list of scopes and the root node of 'npm ls' command for each scope.
 * Used by the npm extractor.
 *
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmProject {

    private List<String> installationArgs;
    private String resolutionRepository;
    private Path workingDir;

    public NpmProject(List<String> installationArgs, String resolutionRepository, Path workingDir) {
        this.installationArgs = installationArgs;
        this.resolutionRepository = resolutionRepository;
        this.workingDir = workingDir;
    }

    public String getResolutionRepository() {
        return resolutionRepository;
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public List<String> getInstallationArgs() {
        return installationArgs;
    }
}
