package org.jfrog.build.extractor.pip;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by Bar Belity on 09/07/2020.
 */
public class PipDriver implements Serializable {
    private static final long serialVersionUID = 1L;

    private CommandExecutor commandExecutor;

    public PipDriver(String executablePath, Map<String, String> env) {
        this.commandExecutor = new CommandExecutor(executablePath, env);
    }

    public String install(File workingDirectory, String url, List<String> commandArgs, Log logger) throws IOException {
        try {
            // Add Artifactory as index to the command.
            List<String> finalArgs = new ArrayList<>(Arrays.asList("install"));
            finalArgs.addAll(commandArgs);
            finalArgs.addAll(Arrays.asList("-i", url));
            // Execute command.
            return runCommand(workingDirectory, finalArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("pip install failed: " + e.getMessage(), e);
        }
    }

    public String freeze(File workingDirectory, Log logger) throws IOException {
        try {
            List<String> finalArgs = new ArrayList<>(Arrays.asList("freeze", "--local"));
            // Execute command.
            return runCommand(workingDirectory, finalArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("pip freeze failed: " + e.getMessage(), e);
        }
    }

    /**
     * Run pip client command with args.
     * Write stdout + stderr to logger, and return the command's result.
     */
    public String runCommand(File workingDirectory, List<String> args, Log logger) throws IOException, InterruptedException {
        CommandResults pipCommandRes = commandExecutor.exeCommand(workingDirectory, args, logger);
        if (!pipCommandRes.isOk()) {
            throw new IOException(pipCommandRes.getErr() + pipCommandRes.getRes());
        }
        return pipCommandRes.getErr() + pipCommandRes.getRes();
    }
}
