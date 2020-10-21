package org.jfrog.build.extractor.nuget.drivers;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.executor.CommandExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DotnetDriver extends ToolchainDriverBase {
    private static final String FLAG_PREFIX = "--";
    private static final String CLEAR_TEXT_PASSWORD_FLAG = FLAG_PREFIX + "store-password-in-clear-text";

    public DotnetDriver(Map<String, String> env, Path workingDirectory, Log logger) {
        super(env, workingDirectory, logger);
        commandExecutor = new CommandExecutor("dotnet", env);
    }

    public String addSource(String configPath, ArtifactoryDependenciesClient client, String repo, String sourceName, String username, String password) throws IOException {
        try {
            String sourceUrl = buildNugetSourceUrl(client, repo);
            List<String> extraArgs = new ArrayList<>();
            extraArgs.addAll(Arrays.asList(FLAG_PREFIX + CONFIG_FILE_FLAG, configPath, FLAG_PREFIX + NAME_FLAG, sourceName, FLAG_PREFIX + USERNAME_FLAG, username, FLAG_PREFIX + PASSWORD_FLAG, password, CLEAR_TEXT_PASSWORD_FLAG));
            return runCommand(new String[]{"nuget", "add", "source", sourceUrl}, extraArgs);
        } catch (Exception e) {
            throw new IOException("dotnet nuget add source failed: " + e.getMessage() + ". Please make sure .NET Core 3.1.200 SDK or above is installed.", e);
        }
    }

    public String globalPackagesCache() throws IOException, InterruptedException {
        // Run `nuget locals globals-packages -list` to get the global packages path
        List<String> args = new ArrayList<>();
        args.add(GLOBAL_PACKAGES_ARG);
        args.add(getFlagSyntax(LIST_FLAG));
        String output = runCommand(new String[]{"nuget", LOCALS_ARG, }, args);
        return output.replaceFirst(GLOBAL_PACKAGES_REGEX, "").trim();
    }

    public String getFlagSyntax(String flagName) {
        return FLAG_PREFIX + flagName;
    }
}