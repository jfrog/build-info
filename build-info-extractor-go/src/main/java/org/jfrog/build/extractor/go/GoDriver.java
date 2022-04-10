package org.jfrog.build.extractor.go;

import org.apache.commons.lang3.StringUtils;
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
            Arrays.asList("list", "-f", "\"{{with .Module}}{{.Path}} {{.Version}}{{end}}\"", "all");
    private static final List<String> GO_MOD_TIDY_CMD = Arrays.asList("mod", "tidy");
    private static final String GO_MOD_GRAPH_CMD = "mod graph";
    private static final String GO_LIST_MODULE_CMD = "list -m";
    private static final String GO_VERSION_CMD = "version";

    private static final long serialVersionUID = 1L;
    private final CommandExecutor commandExecutor;
    private final File workingDirectory;
    private final Log logger;

    public GoDriver(String executablePath, Map<String, String> env, File workingDirectory, Log logger) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, "go"), env);
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    public CommandResults runCmd(String args, boolean verbose) throws IOException {
        List<String> argsList = new ArrayList<>(Arrays.asList(args.split(" ")));
        return runCmd(argsList, verbose);
    }

    /**
     * Run go client cmd with goArs.
     * Write stdout + stderr to logger, and return the command's result.
     */
    public CommandResults runCmd(List<String> args, boolean verbose) throws IOException {
        CommandResults goCmdResult;
        try {
            goCmdResult = commandExecutor.exeCommand(workingDirectory, args, null, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("Go execution failed", e);
        }

        if (!goCmdResult.isOk()) {
            throw new IOException(goCmdResult.getErr());
        }
        if (verbose) {
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

    public CommandResults version(boolean verbose) throws IOException {
        return runCmd(GO_VERSION_CMD, verbose);
    }

    /**
     * Run go mod graph.
     * The output format is:
     * * For direct dependencies:
     * [module-name] [dependency's-module-name]@v[dependency-module-version]
     * * For transitive dependencies:
     * [dependency's-module-name]@v[dependency-module-version] [dependency's-module-name]@v[dependency-module-version]
     *
     * @param verbose - True if should print the results to the log
     * @throws IOException - in case of any I/O error.
     */
    public CommandResults modGraph(boolean verbose) throws IOException {
        return runCmd(GO_MOD_GRAPH_CMD, verbose);
    }

    /**
     * If ignoreErrors=false, run: go mod tidy
     * If ignoreErrors=true, run: go mod tidy -e
     *
     * @param verbose      - True if should print the results to the log
     * @param ignoreErrors - True if errors should be ignored
     * @throws IOException - in case of any I/O error.
     */
    public void modTidy(boolean verbose, boolean ignoreErrors) throws IOException {
        List<String> argsList = new ArrayList<>(GO_MOD_TIDY_CMD);
        if (ignoreErrors) {
            argsList.add("-e");
        }
        runCmd(argsList, verbose);
    }

    /**
     * If ignoreErrors=false, run:
     * go list -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all
     * If ignoreErrors=false, run:
     * go list -e -f "{{with .Module}}{{.Path}} {{.Version}}{{end}}" all
     *
     * @param verbose      - True if should print the results to the log
     * @param ignoreErrors - True if errors should be ignored
     * @throws IOException - in case of any I/O error.
     */
    public CommandResults getUsedModules(boolean verbose, boolean ignoreErrors) throws IOException {
        List<String> argsList = new ArrayList<>(GO_LIST_USED_MODULES_CMD);
        if (ignoreErrors) {
            argsList.add(1, "-e");
        }
        return runCmd(argsList, verbose);
    }

    public String getModuleName() throws IOException {
        CommandResults commandResults = runCmd(GO_LIST_MODULE_CMD, false);
        return commandResults.getRes().trim();
    }
}
