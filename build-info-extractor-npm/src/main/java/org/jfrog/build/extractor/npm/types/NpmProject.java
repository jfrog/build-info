package org.jfrog.build.extractor.npm.types;

import java.nio.file.Path;
import java.util.List;

/**
 * Contains a list of scopes and the root node of 'npm ls' command for each scope.
 * Used by the npm extractor.
 *
 * @author Yahav Itzhak
 */
public class NpmProject {

    private List<String> commandArgs;
    private String resolutionRepository;
    private Path workingDir;
    private boolean ciCommand;

    public NpmProject(List<String> commandArgs, String resolutionRepository, Path workingDir, boolean ciCommand) {
        this.commandArgs = commandArgs;
        this.resolutionRepository = resolutionRepository;
        this.workingDir = workingDir;
        this.ciCommand = ciCommand;
    }

    public String getResolutionRepository() {
        return resolutionRepository;
    }

    public Path getWorkingDir() {
        return workingDir;
    }

    public List<String> getCommandArgs() {
        return commandArgs;
    }

    public boolean isCiCommand() {
        return ciCommand;
    }
}
