package org.jfrog.build.extractor.executor;

import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.util.UrlUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Yahav Itzhak
 */
public class CommandExecutor implements Serializable {
    private static final long serialVersionUID = 1L;

    private String[] env;
    private String executablePath;

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
        if (isWindows()) {
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
     * Execute a command in external process.
     *
     * @param execDir - The execution dir (Usually path to project). Null means current directory.
     * @param args    - Command arguments.
     * @return CommandResults
     */
    public CommandResults exeCommand(File execDir, List<String> args, Log logger) throws InterruptedException, IOException {
        args.add(0, executablePath);
        Process process = null;
        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            CommandResults commandRes = new CommandResults();
            process = runProcess(execDir, args, env, logger);
            StreamReader inputStreamReader = new StreamReader(process.getInputStream());
            StreamReader errorStreamReader = new StreamReader(process.getErrorStream());
            service.submit(inputStreamReader);
            service.submit(errorStreamReader);
            process.waitFor();
            service.shutdown();
            service.awaitTermination(10, TimeUnit.SECONDS);
            commandRes.setRes(inputStreamReader.getOutput());
            commandRes.setErr(errorStreamReader.getOutput());
            commandRes.setExitValue(process.exitValue());
            return commandRes;
        } finally {
            closeStreams(process);
            service.shutdownNow();
        }
    }

    private static void closeStreams(Process process) {
        if (process != null) {
            IOUtils.closeQuietly(process.getInputStream());
            IOUtils.closeQuietly(process.getOutputStream());
            IOUtils.closeQuietly(process.getErrorStream());
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static Process runProcess(File execDir, List<String> args, String[] env, Log logger) throws IOException {
        if (isWindows()) {
            args.add(0, "cmd");
            args.add(1, "/c");
        } else
        if (isMac()) {
            args.add(0, "/bin/sh");
            args.add(1, "-c");
        };
        if (logger != null) {
            logger.info("Executing command: " + UrlUtils.maskCredentialsInUrl(String.join(" ", args)));
        }
        return Runtime.getRuntime().exec(args.toArray(new String[0]), env, execDir);
    }
}
