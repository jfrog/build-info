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

    public String globalPackagesCache() throws IOException, InterruptedException {
        // Run `nuget locals globals-packages -list` to get the global packages path
        List<String> args = new ArrayList<>();
        args.add(GLOBAL_PACKAGES_ARG);
        args.add(getFlagSyntax(LIST_FLAG));
        String output = runCommand(new String[]{"nuget", LOCALS_ARG}, args, null, logger);
        return output.replaceFirst(GLOBAL_PACKAGES_REGEX, "").trim();
    }

    public String getFlagSyntax(String flagName) {
        return FLAG_PREFIX + flagName;
    }
}