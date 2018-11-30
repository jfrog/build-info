package org.jfrog.build.extractor.executor;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class CommandExecutor implements Serializable {
    private static final long serialVersionUID = 1L;

    private String executablePath; // Path to an executable file

    public CommandExecutor(String executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * Execute a command in external process.
     *
     * @param execDir - The execution dir (Usually path to project). Null means current directory.
     * @param args    - Command arguments.
     * @return CommandResults
     */
    public CommandResults exeCommand(File execDir, List<String> args) throws InterruptedException, IOException {
        args.add(0, executablePath);
        Process process = null;
        ExecutorService service = Executors.newFixedThreadPool(2);
        try {
            CommandResults commandRes = new CommandResults();
            process = runProcess(execDir, args);
            StreamReader inputStreamReader = new StreamReader(process.getInputStream());
            StreamReader errorStreamReader = new StreamReader(process.getErrorStream());
            service.submit(inputStreamReader);
            service.submit(errorStreamReader);
            if (process.waitFor(30, TimeUnit.SECONDS)) {
                service.shutdown();
                service.awaitTermination(10, TimeUnit.SECONDS);
                commandRes.setRes(inputStreamReader.getOutput());
                commandRes.setErr(errorStreamReader.getOutput());
            } else {
                commandRes.setErr(String.format("Process execution %s timed out.", String.join(" ", args)));
            }
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

    private static Process runProcess(File execDir, List<String> args) throws IOException {
        String strArgs = String.join(" ", args);
        if (isWindows()) {
            return Runtime.getRuntime().exec(new String[]{"cmd", "/c", strArgs}, null, execDir);
        }
        if (isMac()) {
            return Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", strArgs}, new String[]{"PATH=$PATH:/usr/local/bin"}, execDir);
        }
        // Linux
        return Runtime.getRuntime().exec(args.toArray(new String[0]), null, execDir);
    }

}
