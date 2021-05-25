package org.jfrog.build.extractor.nuget.drivers;

import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.executor.CommandExecutor;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class NugetDriver extends ToolchainDriverBase {
    private static final String FLAG_PREFIX = "-";

    public NugetDriver(Map<String, String> env, Path workingDirectory, Log logger) {
        super(env, workingDirectory, logger);
        commandExecutor = new CommandExecutor("nuget", env);
    }

    public String globalPackagesCache() throws IOException, InterruptedException {
        // Run `nuget locals globals-packages -list` to get the global packages path
        List<String> args = new ArrayList<>();
        args.add(GLOBAL_PACKAGES_ARG);
        args.add(getFlagSyntax(LIST_FLAG));
        String output = runCommand(new String[]{LOCALS_ARG}, args, null, logger);
        return output.replaceFirst(GLOBAL_PACKAGES_REGEX, "").trim();
    }

    public String getFlagSyntax(String flagName) {
        return FLAG_PREFIX + flagName;
    }
}