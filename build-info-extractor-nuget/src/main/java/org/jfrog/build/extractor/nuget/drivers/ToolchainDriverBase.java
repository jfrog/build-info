package org.jfrog.build.extractor.nuget.drivers;

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

abstract public class ToolchainDriverBase implements Serializable {
    private static final String NUGET_PROMPT_ENV_VAR = "NUGET_EXE_NO_PROMPT";
    private static final String ARTIFACTORY_NUGET_API = "/api/nuget/";
    public static final String CONFIG_FILE_FLAG = "configfile";
    public static final String SOURCE_FLAG = "source";
    protected static final String NAME_FLAG = "name";
    protected static final String USERNAME_FLAG = "username";
    protected static final String PASSWORD_FLAG = "password";

    private static final long serialVersionUID = 1L;

    protected static ObjectReader jsonReader = new ObjectMapper().reader();
    protected CommandExecutor commandExecutor;
    protected File workingDirectory;
    protected Log logger;

    public ToolchainDriverBase(Map<String, String> env, Path workingDirectory, Log logger) {
        this.workingDirectory = workingDirectory.toFile();
        this.logger = logger;
        env.put(NUGET_PROMPT_ENV_VAR, "true");
    }

    @SuppressWarnings("unused")
    public boolean isInstalled() {
        try {
            help();
            return true;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    abstract public String addSource(String configPath, ArtifactoryDependenciesClient client, String repo, String sourceName, String username, String password) throws IOException;
    abstract public String globalPackagesCache() throws IOException, InterruptedException;
    abstract public String getFlagSyntax(String flagName);

    public String help() throws IOException, InterruptedException {
        return runCommand(new String[]{"help"}, Collections.emptyList());
    }

    protected String buildNugetSourceUrl(ArtifactoryBaseClient client, String repo) throws Exception {
        URL rtUrl = new URL(client.getArtifactoryUrl());
        URIBuilder sourceUrlBuilder = new URIBuilder()
                .setScheme(rtUrl.getProtocol())
                .setHost(rtUrl.getHost())
                .setPath(rtUrl.getPath() + ARTIFACTORY_NUGET_API + repo);
        return sourceUrlBuilder.build().toURL().toString();
    }

    public void runCmd(String args, List<String> extraArgs, boolean prompt) throws IOException, InterruptedException {
        Log logger = prompt ? this.logger : null;
        String cmdOutput = runCommand(args.split(" "), extraArgs, logger);
        if (prompt) {
            logger.info(cmdOutput);
        }
    }

    protected String runCommand(String[] args, List<String> extraArgs) throws IOException, InterruptedException {
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