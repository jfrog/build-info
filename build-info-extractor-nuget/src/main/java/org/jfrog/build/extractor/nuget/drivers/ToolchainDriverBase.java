package org.jfrog.build.extractor.nuget.drivers;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract public class ToolchainDriverBase implements Serializable {
    public static final String CONFIG_FILE_FLAG = "configfile";
    public static final String SOURCE_FLAG = "source";
    protected static final String NAME_FLAG = "name";
    protected static final String USERNAME_FLAG = "username";
    protected static final String PASSWORD_FLAG = "password";
    protected static final String LIST_FLAG = "list";
    protected static final String LOCALS_ARG = "locals";
    protected static final String GLOBAL_PACKAGES_ARG = "global-packages";
    protected static final String GLOBAL_PACKAGES_REGEX = "^global-packages:";
    private static final String NUGET_PROMPT_ENV_VAR = "NUGET_EXE_NO_PROMPT";
    private static final String ARTIFACTORY_NUGET_API = "/api/nuget/";
    private static final String ARTIFACTORY_NUGET_API_V3 = ARTIFACTORY_NUGET_API + "v3/";
    private static final String V3 = "v3";
    private static final long serialVersionUID = 1L;

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

    abstract public String globalPackagesCache() throws IOException, InterruptedException;

    abstract public String getFlagSyntax(String flagName);

    public String help() throws IOException, InterruptedException {
        return runCommand(new String[]{"help"}, Collections.emptyList(), null, logger);
    }

    public String buildNugetSourceUrl(ArtifactoryBaseClient client, String repo, String apiProtocol) throws Exception {
        URL rtUrl = new URL(client.getArtifactoryUrl());
        String nugetApi = apiProtocol.equalsIgnoreCase(V3) ? ARTIFACTORY_NUGET_API_V3 : ARTIFACTORY_NUGET_API;
        URIBuilder sourceUrlBuilder = new URIBuilder()
                .setScheme(rtUrl.getProtocol())
                .setHost(rtUrl.getHost())
                .setPath(rtUrl.getPath() + nugetApi + repo)
                .setPort(rtUrl.getPort());
        return sourceUrlBuilder.build().toURL().toString();
    }

    public void runCmd(String args, List<String> extraArgs, List<String> credentials, boolean prompt) throws IOException, InterruptedException {
        Log logger = prompt ? this.logger : null;
        String cmdOutput = runCommand(args.split(" "), extraArgs, credentials, logger);
        if (prompt) {
            logger.info(cmdOutput);
        }
    }

    /**
     * Run .NET/Nuget command.
     *
     * @param args        - Base args, such as "nuget add source source-url"
     * @param extraArgs   - Extra args, such as "--config=config-path --name=source-name"
     * @param credentials - Credentials, such as "--username=username --password=password"
     * @param logger      - The logger of the command
     * @return the output.
     * @throws IOException          if the command execution returned an error.
     * @throws InterruptedException if the command execution interrupted.
     */
    protected String runCommand(String[] args, List<String> extraArgs, List<String> credentials, Log logger) throws IOException, InterruptedException {
        List<String> finalArgs = Stream.concat(Arrays.stream(args), extraArgs.stream()).collect(Collectors.toList());
        CommandResults nugetCommandRes = commandExecutor.exeCommand(workingDirectory, finalArgs, credentials, logger);
        if (!nugetCommandRes.isOk()) {
            throw new IOException(nugetCommandRes.getErr() + nugetCommandRes.getRes());
        }
        return nugetCommandRes.getErr() + nugetCommandRes.getRes();
    }
}