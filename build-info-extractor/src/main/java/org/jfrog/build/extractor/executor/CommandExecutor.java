package org.jfrog.build.extractor.executor;

import org.apache.commons.io.IOUtils;
import org.jfrog.build.api.util.Log;

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
        Map<String, String> envMap = new HashMap<>(System.getenv());
        if (env != null) {
            envMap.putAll(env);
        }
        fixPathEnv(envMap);
        this.env = envMap.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue()).toArray(String[]::new);
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
            path = path.replaceAll(":", File.pathSeparator);
        } else {
            path = path.replaceAll(";", File.pathSeparator) + ":/usr/local/bin";
        }
        env.replace("PATH", path);
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
        String strArgs = String.join(" ", args);
        if (logger != null) {
            logger.info("Executing command: " + strArgs);
        }
        if (isWindows()) {
            return Runtime.getRuntime().exec(new String[]{"cmd", "/c", strArgs}, env, execDir);
        }
        if (isMac()) {
            return Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", strArgs}, env, execDir);
        }
        // Linux
        return Runtime.getRuntime().exec(args.toArray(new String[0]), env, execDir);
    }

}
