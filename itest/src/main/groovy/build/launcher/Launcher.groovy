package build.launcher

import build.TestInputContributor
import com.google.common.collect.Maps
import com.google.common.collect.Lists

/**
 * @author Lior Hasson
 */
abstract class Launcher implements TestInputContributor{
    static final OS_WIN = System.getProperty("os.name").contains("Windows")
    protected def processEnvironment = Maps.newHashMap()
    protected def cmd = []
    protected def commandPath
    protected def projectFilePath
    protected Map<String, Object> envVars = Maps.newHashMap()
    protected Map<String, Object> systemProps = Maps.newHashMap()
    protected List<String> tasks = Lists.newLinkedList()
    protected List<String> switches = Lists.newLinkedList()
    protected List<String> buildToolVersions = Lists.newLinkedList()
    protected File workingDirectory;

    Launcher(projectFilePath) {
        this.projectFilePath = projectFilePath
    }

    Launcher(commandPath, projectFilePath) {
        this.commandPath = commandPath
        this.projectFilePath = projectFilePath
    }

    protected abstract void createCmd()

    protected abstract def buildToolVersionHandler()

    private def getCmd() {
        if (cmd.isEmpty()) {
            createCmd()
        }
        cmd
    }

    void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory
    }

    Launcher addEnvVar(String name, String value) {
        envVars.put(name, value)
        this
    }

    Launcher addSystemProp(String name, String value) {
        systemProps.put(name, value)
        this
    }

    Launcher addTask(String task) {
        tasks.add(task)
        this
    }

    Launcher addSwitch(String gradleSwitch) {
        switches.add(gradleSwitch)
        this
    }

    Launcher addToolVersions(String version) {
        buildToolVersions.add(version)
        this
    }

    protected def systemPropsToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for (var in systemProps) {
            def key = var.key.startsWith("-D") ? var.key : "-D${var.key}"
            sb.append(key).append("=").append(var.value)
            if (c++ < systemProps.size() - 1) {
                sb.append(" ")
            }
        }
        sb
    }

    protected def tasksToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for (task in tasks) {
            sb.append(task)
            if (c++ < tasks.size() - 1) {
                sb.append(" ")
            }
        }
        sb
    }

    protected def switchesToString() {
        StringBuilder sb = new StringBuilder()
        int c = 0;
        for(toolSwitch in switches) {
            toolSwitch = toolSwitch.startsWith("--") ? toolSwitch : "--${toolSwitch}"
            sb.append(toolSwitch)
            if (c++ < switches.size()-1) {
                sb.append(" ")
            }
        }
        sb
    }

    @Override
    def contribute() {
        //exit code for a set of commands
        int totalExitCode = 0
        Process p
        getCmd().each {
            def command = it
            try {
                ProcessBuilder pb = new ProcessBuilder(Lists.newArrayList(command.split("\\s+")))
                pb.environment().putAll(processEnvironment)

                if (workingDirectory) {
                    pb.directory(workingDirectory)
                }

                println "Launching build process: $command"
                p = pb.start()

                LogPrinter inputPrinter = new LogPrinter(p.getInputStream())
                LogPrinter errorPrinter = new LogPrinter(p.getErrorStream())

                Thread t1 = new Thread(inputPrinter)
                t1.start()
                Thread t2 = new Thread(errorPrinter)
                t2.start()

                t1.join()
                t2.join()

                p.waitFor()

                println "Build tool process finished with exit code ${p.exitValue()}"
                totalExitCode += p.exitValue()
            } finally {
                if (p != null) {
                    if (p.getInputStream() != null) {
                        p.getInputStream().close()
                    }
                    if (p.getOutputStream() != null) {
                        p.getOutputStream().close()
                    }
                    if (p.getErrorStream() != null) {
                        p.getErrorStream().close()
                    }
                }
            }
        }
        totalExitCode
    }

    private static class LogPrinter implements Runnable {
        private InputStream inputStream

        LogPrinter(InputStream inputStream) {
            this.inputStream = inputStream
        }

        @Override
        public void run() {
            final def processReader =
                    new BufferedReader(new InputStreamReader(inputStream))

            processReader.withReader {
                def line
                while ((line = it.readLine()) != null) {
                    println(line)
                }
            }
        }
    }
}
