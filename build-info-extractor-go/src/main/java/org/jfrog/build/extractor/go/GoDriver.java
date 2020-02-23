package org.jfrog.build.extractor.go;

import org.apache.commons.lang.StringUtils;
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
 * Created by Bar Belity on 13/02/2020.
 */
public class GoDriver implements Serializable {
    private static final String GO_VERSION_CMD = "version";
    private static final String GO_MOD_GRAPH_CMD = "mod graph";
    private static final String GO_LIST_MODULE = "list -m";

    private static final long serialVersionUID = 1L;
    private CommandExecutor commandExecutor;
    private File workingDirectory;
    private Log logger;

    public GoDriver(String executablePath, Map<String, String> env, File workingDirectory, Log logger) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, "go"), env);
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    public CommandResults runGoCmd(String args, boolean prompt) throws IOException {
        List argsList = new ArrayList<>(Arrays.asList(args.split(" ")));
        return runGoCmd(argsList, prompt);
    }

    /**
     * Run go client cmd with goArs.
     * Write stdout + stderr to logger, and return the command's result.
     */
    public CommandResults runGoCmd(List<String> args, boolean prompt) throws IOException {
        CommandResults goCmdResult;
        try {
            goCmdResult = commandExecutor.exeCommand(workingDirectory, args, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("Go execution failed", e);
        }

        if (!goCmdResult.isOk()) {
            throw new IOException(goCmdResult.getErr());
        }
        if (prompt) {
            logger.info(goCmdResult.getErr() + goCmdResult.getRes());
        }
        return goCmdResult;
    }

    @SuppressWarnings("unused")
    public boolean isGoInstalled() {
        try {
            goVersion(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public CommandResults goVersion(boolean prompt) throws IOException {
        return runGoCmd(GO_VERSION_CMD, prompt);
    }

    public CommandResults goModGraph(boolean prompt) throws IOException {
        return runGoCmd(GO_MOD_GRAPH_CMD, prompt);
    }

    public String getModuleName() throws IOException {
        CommandResults commandResults = runGoCmd(GO_LIST_MODULE, false);
        return commandResults.getRes().trim();
    }
}
