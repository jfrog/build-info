package org.jfrog.build.extractor.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Yahav Itzhak on 15 Nov 2018.
 */
public class NpmDriver implements Serializable {
    private static final long serialVersionUID = 1L;

    private static ObjectReader jsonReader = new ObjectMapper().reader();
    private CommandExecutor commandExecutor;

    public NpmDriver(String executablePath) {
        this.commandExecutor = new CommandExecutor(StringUtils.defaultIfEmpty(executablePath, "npm"));
    }

    @SuppressWarnings("unused")
    public boolean isNpmInstalled() {
        try {
            version(new File(""));
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public void install(File workingDirectory, List<String> extraArgs) throws IOException {
        try {
            runCommand(workingDirectory, new String[]{"i"}, extraArgs);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm install failed: " + e.getMessage(), e);
        }
    }

    public void pack(File workingDirectory, List<String> extraArgs) throws IOException {
        try {
            runCommand(workingDirectory, new String[]{"pack"}, extraArgs);
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
            CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, args);
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
        return runCommand(workingDirectory, new String[]{"c", "ls", "--json"}, extraArgs);
    }

    private String runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, finalArgs);
        if (!npmCommandRes.isOk()) {
            throw new IOException(npmCommandRes.getErr());
        }
        return npmCommandRes.getRes();
    }
}