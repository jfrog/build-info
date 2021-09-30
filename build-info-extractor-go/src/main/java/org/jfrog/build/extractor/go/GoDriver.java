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
    private static final List<String> GO_LIST_USED_MODULES_CMD =
            Arrays.asList("list", "-f" ,"\"{{with .Module}}{{.Path}} {{.Version}}{{end}}\"", "all");
    private static final String GO_MOD_GRAPH_CMD = "mod graph";
    private static final String GO_MOD_TIDY_CMD = "mod tidy";
    private static final String GO_VERSION_CMD = "version";
    private static final String GO_LIST_MODULE = "list -m";

    private static final long serialVersionUID = 1L;
    private final CommandExecutor commandExecutor;
    private final File workingDirectory;
    private final Log logger;

    public GoDriver(String executablePath, Map<String, String> env, File workingDirectory, Log logger) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, "go"), env);
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    public CommandResults runCmd(String args, boolean prompt) throws IOException {
        List<String> argsList = new ArrayList<>(Arrays.asList(args.split(" ")));
        return runCmd(argsList, prompt);
    }

    /**
     * Run go client cmd with goArs.
     * Write stdout + stderr to logger, and return the command's result.
     */
    public CommandResults runCmd(List<String> args, boolean prompt) throws IOException {
        CommandResults goCmdResult;
        try {
            goCmdResult = commandExecutor.exeCommand(workingDirectory, args, null, logger);
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
    public boolean isInstalled() {
        try {
            version(false);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public CommandResults version(boolean prompt) throws IOException {
        return runCmd(GO_VERSION_CMD, prompt);
    }

    public CommandResults modGraph(boolean prompt) throws IOException {
        return runCmd(GO_MOD_GRAPH_CMD, prompt);
    }

    public void modTidy(boolean prompt) throws IOException {
        runCmd(GO_MOD_TIDY_CMD, prompt);
    }

    public CommandResults getUsedModules(boolean prompt) throws IOException {
        List<String> argsList = new ArrayList<>(GO_LIST_USED_MODULES_CMD);
        return runCmd(argsList, prompt);
    }

    public String getModuleName() throws IOException {
        CommandResults commandResults = runCmd(GO_LIST_MODULE, false);
        return commandResults.getRes().trim();
    }
}
