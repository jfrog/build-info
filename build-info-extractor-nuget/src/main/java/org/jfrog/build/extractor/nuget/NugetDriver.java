package org.jfrog.build.extractor.nuget;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.http.client.utils.URIBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBaseClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NugetDriver implements Serializable {
    private static final String NUGET_PROMPT_ENV_VAR = "NUGET_EXE_NO_PROMPT";
    private static final String ARTIFACTORY_NUGET_API = "/api/nuget/";
    public static final String SOURCE_FLAG = "-source";
    private static final String NAME_FLAG = "-name";
    private static final String USERNAME_FLAG = "-username";
    private static final String PASSWORD_FLAG = "-password";
    private static final String CONFIG_FILE_FLAG = "-configfile";

    private static final long serialVersionUID = 1L;

    private static ObjectReader jsonReader = new ObjectMapper().reader();
    private CommandExecutor commandExecutor;
    private File workingDirectory;
    private Log logger;

    public NugetDriver(Map<String, String> env, Path workingDirectory, Log logger) {
        env.put(NUGET_PROMPT_ENV_VAR, "true");
        this.commandExecutor = new CommandExecutor("nuget", env);
        this.workingDirectory = workingDirectory.toFile();
        this.logger = logger;
    }

    @SuppressWarnings("unused")
    public boolean isNugetInstalled() {
        try {
            help(null);
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    public String addSource(String configPath, ArtifactoryDependenciesClient client, String repo, String sourceName, String username, String password) throws IOException {
        try {
            String sourceUrl = buildNugetSourceUrl(client, repo);
            List<String> extraArgs = new ArrayList<>();
            extraArgs.addAll(Arrays.asList(CONFIG_FILE_FLAG, configPath, SOURCE_FLAG, sourceUrl, NAME_FLAG, sourceName, USERNAME_FLAG, username, PASSWORD_FLAG, password));
            return runCommand(new String[]{"sources", "add"}, extraArgs);
        } catch (Exception e) {
            throw new IOException("nuget restore failed: " + e.getMessage(), e);
        }
    }

    private String buildNugetSourceUrl(ArtifactoryBaseClient client, String repo) throws Exception {
        URL rtUrl = new URL(client.getArtifactoryUrl());
        URIBuilder sourceUrlBuilder = new URIBuilder()
                .setScheme(rtUrl.getProtocol())
                .setHost(rtUrl.getHost())
                .setPath(rtUrl.getPath() + ARTIFACTORY_NUGET_API + repo);
        return sourceUrlBuilder.build().toURL().toString();
    }

//    public JsonNode list(File workingDirectory, List<String> extraArgs) throws IOException {
//        List<String> args = new ArrayList<>();
//        args.add("ls");
//        args.add("--json");
//        args.addAll(extraArgs);
//        try {
//            CommandResults npmCommandRes = commandExecutor.exeCommand(workingDirectory, args, null);
//            String res = StringUtils.isBlank(npmCommandRes.getRes()) ? "{}" : npmCommandRes.getRes();
//            return jsonReader.readTree(res);
//        } catch (IOException | InterruptedException e) {
//            throw new IOException("npm ls failed", e);
//        }
//    }

    public String help(File workingDirectory) throws IOException, InterruptedException {
        return runCommand(new String[]{"help"}, Collections.emptyList());
    }

    public void runCmd(String args, List<String> extraArgs, boolean prompt) throws IOException, InterruptedException {
        Log logger = prompt ? this.logger : null;
        String cmdOutput = runCommand(args.split(" "), extraArgs, logger);
        if (prompt) {
            logger.info(cmdOutput);
        }
    }

    public String globalPackagesCache() throws IOException, InterruptedException {
        // Run `nuget locals globals-packages -list` to get the global packages path
        List<String> args = new ArrayList<>();
        args.add("global-packages");
        args.add("-list");
        String output = runCommand(new String[]{"locals", }, args);
        return output.replaceFirst("^global-packages:", "").trim();
    }

    private String runCommand(String[] args, List<String> extraArgs) throws IOException, InterruptedException {
        return runCommand(args, extraArgs, null);
    }
    private String runCommand(String[] args, List<String> extraArgs, Log logger) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults nugetCommandRes = commandExecutor.exeCommand(workingDirectory, finalArgs, logger);
        if (!nugetCommandRes.isOk()) {
            throw new IOException(nugetCommandRes.getErr() + nugetCommandRes.getRes());
        }
        return nugetCommandRes.getErr() + nugetCommandRes.getRes();
    }
}