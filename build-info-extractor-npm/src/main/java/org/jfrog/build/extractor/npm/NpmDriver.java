package org.jfrog.build.extractor.npm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
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

    public NpmDriver(Map<String, String> env) {
        this.commandExecutor = new CommandExecutor("npm", env);
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
        CommandResults results;

        try {
            results = runCommand(workingDirectory, new String[]{"i"}, extraArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm install failed: " + e.getMessage(), e);
        }

        return results.getErr() + results.getRes();
    }

    public String ci(File workingDirectory, List<String> extraArgs, Log logger) throws IOException {
        CommandResults results;

        try {
            results = runCommand(workingDirectory, new String[]{"ci"}, extraArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm ci failed: " + e.getMessage(), e);
        }

        return results.getErr() + results.getRes();
    }

    /**
     * Runs 'npm pack' command.
     * @return the name of the new package file.
     */
    public String pack(File workingDirectory, List<String> extraArgs, Log logger) throws IOException {
        CommandResults results;

        try {
            results = runCommand(workingDirectory, new String[]{"pack"}, extraArgs, logger);
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm pack failed: " + e.getMessage(), e);
        }

        logger.info(results.getErr() + results.getRes());
        String fileName = results.getRes().trim();
        return fileName;
    }

    public JsonNode list(File workingDirectory, List<String> extraArgs) throws IOException {
        List<String> args = new ArrayList<>();
        args.add("ls");
        args.add("--json");
        args.add("--all");
        args.addAll(extraArgs);
        try {
            CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, args, null, null);
            String res = StringUtils.isBlank(npmCommandRes.getRes()) ? "{}" : npmCommandRes.getRes();
            JsonNode npmLsResults = jsonReader.readTree(res);
            if (!npmCommandRes.isOk() && !npmLsResults.has("problems")) {
                ((ObjectNode) npmLsResults).put("problems", npmCommandRes.getErr());
            }
            return npmLsResults;
        } catch (IOException | InterruptedException e) {
            throw new IOException("npm ls failed", e);
        }
    }

    public String version(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(workingDirectory, new String[]{"--version"}, Collections.emptyList()).getRes();
    }

    public String configList(File workingDirectory, List<String> extraArgs, Log logger) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>(extraArgs);
        CommandResults res = runCommand(workingDirectory, new String[]{"c", "ls"}, args);

        if (!Strings.isNullOrEmpty(res.getErr())) {
            logger.warn(res.getErr());
        }

        return res.getRes();
    }

    private CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        return runCommand(workingDirectory, args, extraArgs, null);
    }

    private CommandResults runCommand(File workingDirectory, String[] args, List<String> extraArgs, Log logger) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, finalArgs, null, logger);

        if (!npmCommandRes.isOk()) {
            throw new IOException(npmCommandRes.getErr() + npmCommandRes.getRes());
        }

        return npmCommandRes;
    }
}