package org.jfrog.build.extractor.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yahav Itzhak
 */
public class NpmDriver implements Serializable {
    private static final long serialVersionUID = 1L;

    private static ObjectReader jsonReader = new ObjectMapper().reader();
    private CommandExecutor commandExecutor;

    public NpmDriver(String executablePath, Map<String, String> env) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, "npm"), env);
    }

    @SuppressWarnings("unused")
    public boolean isNpmInstalled() {
        try {
            version(null);
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public String install(File workingDirectory, List<String> extraArgs, Log logger) throws IOException {
        try {
            return runCommand(workingDirectory, new String[]{"i"}, extraArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm install failed: " + e.getMessage(), e);
        }
    }

    public String pack(File workingDirectory, List<String> extraArgs) throws IOException {
        try {
            return runCommand(workingDirectory, new String[]{"pack"}, extraArgs);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm pack failed: " + e.getMessage(), e);
        }
    }

    public JsonNode list(File workingDirectory, List<String> extraArgs) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("ls");
        args.add("--json");
        args.addAll(extraArgs);
        try {
            CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, args, null);
            String res = StringUtils.isBlank(npmCommandRes.getRes()) ? "{}" : npmCommandRes.getRes();
            return jsonReader.readTree(res);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm ls failed", e);
        }
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, new String[]{"--version"}, Collections.emptyList());
    }

    public String configList(File workingDirectory, List<String> extraArgs) throws IOException, InterruptedException {
        // We're adding the -s option to the command, to make sure the command output can be parsed properly.
        List<String> args = new ArrayList<>(extraArgs);
        // We will temporarily remove this argument because we do want to get json output for the config list cmd
        // The required value will be restore later while writing the npmrc file.
        args.remove("--json=false");
        args.add("-s");
        return runCommand(workingDirectory, new String[]{"c", "ls", "--json"}, args);
    }

    private String runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        return runCommand(workingDirectory, args, extraArgs, null);
    }

    private String runCommand(File workingDirectory, String[] args, List<String> extraArgs, Log logger) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, finalArgs, logger);
        if (!npmCommandRes.isOk()) {
            throw new IOException(npmCommandRes.getErr() + npmCommandRes.getRes());
        }
        return npmCommandRes.getErr() + npmCommandRes.getRes();
    }
}