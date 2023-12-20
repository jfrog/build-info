package org.jfrog.build.extractor.go;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
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
    private static final String GO_GET_CMD = "get";
    private static final String GO_LIST_MODULE_CMD = "list -m";
    private static final String GO_VERSION_CMD = "version";
    private static final String DEFAULT_EXECUTABLE_NAME = "go";

    private static final long serialVersionUID = 1L;
    private final CommandExecutor commandExecutor;
    private final File workingDirectory;
    private final Log logger;

    public GoDriver(String executablePath, Map<String, String> env, File workingDirectory, Log logger) {
        this.commandExecutor = generateCommandExecutor(executablePath, env);
        this.workingDirectory = workingDirectory;
        this.logger = logger;
    }

    /**
     * Generate a new mutable copy of environment variables map with the Go executable directory path inserted to the beginning of the Path.
     *
     * @param executablePath Go executable path
     * @param env            Environment variables map
     * @return a new Environment variables map
     */
    static Map<String, String> generateWindowsEnv(String executablePath, Map<String, String> env) {
        // If executablePath ends with "go" or "go.exe" - remove it from the directory path
        executablePath = StringUtils.removeEnd(executablePath, ".exe");
        executablePath = StringUtils.removeEnd(executablePath, DEFAULT_EXECUTABLE_NAME);

        // Insert the Go executable directory path to the beginning of the Path environment variable
        // Make sure to copy the environment variables map to avoid changing the original map or in case it is immutable
        Map<String, String> newEnv = Maps.newHashMap(env);
        String windowsPathEnvKey = "Path";
        if (newEnv.containsKey(windowsPathEnvKey)) {
            newEnv.put(windowsPathEnvKey, executablePath + File.pathSeparator + newEnv.get(windowsPathEnvKey));
        } else {
            newEnv.put(windowsPathEnvKey, executablePath);
        }
        return newEnv;
    }

    /**
     * Create a CommandExecutor with the given executable path and environment variables.
     *
     * @param executablePath Go executable path
     * @param env            Environment variables map
     * @return CommandExecutor
     */
    private static CommandExecutor generateCommandExecutor(String executablePath, Map<String, String> env) {
        if (!SystemUtils.IS_OS_WINDOWS || StringUtils.isBlank(executablePath) || StringUtils.equals(executablePath, DEFAULT_EXECUTABLE_NAME) || env == null) {
            return new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, DEFAULT_EXECUTABLE_NAME), env);
        }
        // Handling Windows case with a given executable path:
        // A bug was identified for the Go executable in Windows where the executable path may be incorrectly parsed
        // as two command arguments when the path contains spaces (e.g., "C:\Program Files\Go\bin\go.exe").
        return new CommandExecutor(DEFAULT_EXECUTABLE_NAME, generateWindowsEnv(executablePath, env));
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
     * Run go get.
     *
     * @param componentId - Component ID string. ( Example: github.com/jfrog/build-info-go@v1.8.7 )
     * @param verbose     - True if should print the results to the log
     * @throws IOException - in case of any I/O error.
     */
    public void get(String componentId, boolean verbose) throws IOException {
        List<String> argsList = new ArrayList<>(Arrays.asList(GO_GET_CMD, componentId));
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
     * @param dontBuildVcs - Skip VCS stamping - can be used only on Go later than 1.18
     * @throws IOException - in case of any I/O error.
     */
    public CommandResults getUsedModules(boolean verbose, boolean ignoreErrors, boolean dontBuildVcs) throws IOException {
        List<String> argsList = new ArrayList<>(GO_LIST_USED_MODULES_CMD);
        if (dontBuildVcs) {
            argsList.add(1, "-buildvcs=false");
        }
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
