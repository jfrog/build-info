package org.jfrog.build.extractor.nuget.drivers;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.executor.CommandExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class NugetDriver extends ToolchainDriverBase {
    private static final String FLAG_PREFIX = "-";

    public NugetDriver(Map<String, String> env, Path workingDirectory, Log logger) {
        super(env, workingDirectory, logger);
        commandExecutor = new CommandExecutor("nuget", env);
    }

    public String addSource(String configPath, ArtifactoryDependenciesClient client, String repo, String sourceName, String username, String password) throws IOException {
        try {
            String sourceUrl = buildNugetSourceUrl(client, repo);
            List<String> extraArgs = new ArrayList<>();
            extraArgs.addAll(Arrays.asList(FLAG_PREFIX + CONFIG_FILE_FLAG, configPath, FLAG_PREFIX + SOURCE_FLAG, sourceUrl, FLAG_PREFIX + NAME_FLAG, sourceName, FLAG_PREFIX + USERNAME_FLAG, username, FLAG_PREFIX + PASSWORD_FLAG, password));
            return runCommand(new String[]{"sources", "add"}, extraArgs);
        } catch (Exception e) {
            throw new IOException("nuget sources add failed: " + e.getMessage(), e);
        }
    }

    public String globalPackagesCache() throws IOException, InterruptedException {
        // Run `nuget locals globals-packages -list` to get the global packages path
        List<String> args = new ArrayList<>();
        args.add(GLOBAL_PACKAGES_ARG);
        args.add(getFlagSyntax(LIST_FLAG));
        String output = runCommand(new String[]{LOCALS_ARG, }, args);
        return output.replaceFirst(GLOBAL_PACKAGES_REGEX, "").trim();
    }

    public String getFlagSyntax(String flagName) {
        return FLAG_PREFIX + flagName;
    }
}