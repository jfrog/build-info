package org.jfrog.build.extractor.executor;

import org.apache.commons.lang.SystemUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.util.UrlUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Yahav Itzhak
 */
public class CommandExecutor implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String[] env;
    private final String executablePath;

    /**
     * @param executablePath - Executable path.
     * @param env            - Environment variables to use during execution.
     */
    public CommandExecutor(String executablePath, Map<String, String> env) {
        this.executablePath = executablePath;
        Map<String, String> finalEnvMap = new HashMap<>(System.getenv());
        if (env != null) {
            Map<String, String> fixedEnvMap = new HashMap<>(env);
            fixPathEnv(fixedEnvMap);
            finalEnvMap.putAll(fixedEnvMap);
        }
        this.env = finalEnvMap.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
    }

    /**
     * 1. Use correct file separator.
     * 2. In unix, append ":/usr/local/bin" to PATH environment variable.
     *
     * @param env - Environment variables map.
     */
    private void fixPathEnv(Map<String, String> env) {
        String path = env.get("PATH");
        if (path == null) {
            return;
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            path = getFixedWindowsPath(path);
        } else {
            path = path.replaceAll(";", File.pathSeparator) + ":/usr/local/bin";
        }
        env.replace("PATH", path);
    }

    /**
     * Fix the PATH value to be valid for execution on a Windows machine.
     * Take care of a case when either non-Windows or Windows environment-variables are received.
     * Examples:
     * "C:\my\first;Drive:\my\second" returns "C:\my\first;Drive:\my\second"
     * "/Users/my/first:/Users/my/second" returns "/Users/my/first;/Users/my/second"
     *
     * @param path - Value of PATH environment variable.
     * @return Fixed PATH value.
     */
    static String getFixedWindowsPath(String path) {
        String[] pathParts = path.split(";");
        String[] newPathParts = new String[pathParts.length];
        for (int index = 0; index < pathParts.length; index++) {
            String part = pathParts[index];
            int backSlashIndex = part.indexOf('\\');
            if (backSlashIndex < 0) {
                newPathParts[index] = part.replaceAll(":", ";");
                continue;
            }
            String startPart = part.substring(0, backSlashIndex);
            String endPart = part.substring(backSlashIndex);
            String newPart = startPart + endPart.replaceAll(":", ";");
            newPathParts[index] = newPart;
        }
        return String.join(";", newPathParts);
    }

    /**
     * Replace credentials in the string command by '***'.
     *
     * @param command     - The command
     * @param credentials - The credentials list
     * @return masked command.
     */
    static String maskCredentials(String command, List<String> credentials) {
        if (credentials == null || credentials.isEmpty()) {
            return command;
        }
        // The mask pattern is a regex, which is used to mask all credentials
        String maskPattern = String.join("|", credentials);
        return command.replaceAll(maskPattern, "***");
    }

    /**
     * Execute a command in external process.
     *
     * @param execDir     - The execution dir (Usually path to project). Null means current directory.
     * @param args        - Command arguments.
     * @param credentials - If specified, the credentials will be concatenated to the other commands.
     *                    The credentials will be makes in the log output.
     * @param logger      - The logger which will log the running command.
     * @return CommandResults object
     */
    public CommandResults exeCommand(File execDir, List<String> args, List<String> credentials, Log logger) throws InterruptedException, IOException {
        args.add(0, executablePath);
        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            CommandResults commandRes = new CommandResults();
            Process process = runProcess(execDir, args, credentials, env, logger);
            // The output stream is not necessary in non-interactive scenarios, therefore we can close it now.
            process.getOutputStream().close();
            try (InputStream inputStream = process.getInputStream();
                 InputStream errorStream = process.getErrorStream()) {
                StreamReader inputStreamReader = new StreamReader(inputStream);
                StreamReader errorStreamReader = new StreamReader(errorStream);
                service.submit(inputStreamReader);
                service.submit(errorStreamReader);
                process.waitFor();
                service.shutdown();
                boolean terminatedProperly = service.awaitTermination(10, TimeUnit.SECONDS);
                if (!terminatedProperly && logger != null) {
                    logger.error("Process '" + String.join(" ", args) + "' had been terminated forcibly after timeout of 10 seconds.");
                }
                commandRes.setRes(inputStreamReader.getOutput());
                commandRes.setErr(errorStreamReader.getOutput());
                commandRes.setExitValue(process.exitValue());
                return commandRes;
            }
        } finally {
            service.shutdownNow();
        }
    }

    private static Process runProcess(File execDir, List<String> args, List<String> credentials, String[] env, Log logger) throws IOException {
        if (credentials != null) {
            args.addAll(credentials);
        }
        if (SystemUtils.IS_OS_WINDOWS) {
            args.addAll(0, Arrays.asList("cmd", "/c"));
        } else if (SystemUtils.IS_OS_MAC) {
            // In MacOS, the arguments for '/bin/sh -c' must be a single string. For example "/bin/sh","-c", "npm i".
            String strArgs = String.join(" ", args);
            args = new ArrayList<String>() {{
                add("/bin/sh");
                add("-c");
                add(strArgs);
            }};
        }
        logCommand(logger, args, credentials);
        return Runtime.getRuntime().exec(args.toArray(new String[0]), env, execDir);
    }

    private static void logCommand(Log logger, List<String> args, List<String> credentials) {
        if (logger == null) {
            return;
        }
        // Mask credentials in URL
        String output = UrlUtils.maskCredentialsInUrl(String.join(" ", args));

        // Mask credentials arguments
        output = maskCredentials(output, credentials);

        logger.info("Executing command: " + output);
    }
}
