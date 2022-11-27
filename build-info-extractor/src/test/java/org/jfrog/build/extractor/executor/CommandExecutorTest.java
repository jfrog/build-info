package org.jfrog.build.extractor.executor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.util.NullLog;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Created by Bar Belity on 04/08/2020.
 */
public class CommandExecutorTest {

    @DataProvider
    private Object[][] getFixedWindowsPathProvider() {
        return new Object[][]{
                {"C:\\my\\first;Drive:\\my\\second", "C:\\my\\first;Drive:\\my\\second"},
                {"C:\\my\\first;Drive:\\my\\second:\\my\\third", "C:\\my\\first;Drive:\\my\\second;\\my\\third"},
                {"/Users/my/first:/Users/my/second", "/Users/my/first;/Users/my/second"},
                {"/Users/my/first:/Users/my/sec;ond", "/Users/my/first;/Users/my/sec;ond"},
                {"/Users/my/first:/Users/my/sec;ond:C:\\my\\third", "/Users/my/first;/Users/my/sec;ond:C:\\my\\third"},
        };
    }

    @Test(dataProvider = "getFixedWindowsPathProvider")
    public void getFixedWindowsPathTest(String path, String expected) {
        String actual = CommandExecutor.getFixedWindowsPath(path);
        assertEquals(actual, expected);
    }

    @DataProvider
    private Object[][] maskCredentialsPathProvider() {
        return new Object[][]{
                {"command with no secrets", "command with no secrets", Lists.newArrayList()},
                {"command with no secrets", "command with no secrets", null},
                {"command with one secret --password=abc123", "command with one secret ***", Lists.newArrayList("--password=abc123")},
                {"command with two secrets --username=Grogu --password=abc123", "command with two secrets *** ***", Lists.newArrayList("--username=Grogu", "--password=abc123")}
        };
    }

    @Test(dataProvider = "maskCredentialsPathProvider")
    public void testMaskCredentials(String command, String expected, List<String> credentials) {
        assertEquals(CommandExecutor.maskCredentials(command, credentials), expected);
    }

    @Test
    public void testExeCommand() {
        List<String> args = new ArrayList<>();
        args.add("--version");
        CommandExecutor executor = new CommandExecutor("git", System.getenv());
        try {
            CommandResults results = executor.exeCommand(null, args, null, new NullLog());
            assertTrue(results.isOk(), results.getErr() + results.getRes());
        } catch (InterruptedException | IOException e) {
            fail(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @Test
    public void testExeCommandWithSpaces() throws IOException, InterruptedException {
        Path tmpDir = Files.createTempDirectory("testEscapeSpacesInPath");
        try {
            Path testDir = Files.createDirectories(tmpDir.resolve("Kuiil space"));
            File executableFile = Files.createFile(testDir.resolve("ship.bat")).toFile();
            assertTrue(executableFile.setExecutable(true));
            FileUtils.writeStringToFile(executableFile, "echo \"I have spoken\"", StandardCharsets.UTF_8);

            // Run executable in "Kuiil space/ship.bat" with no arguments and make sure it prints "I have spoken"
            CommandExecutor commandExecutor = new CommandExecutor(executableFile.getAbsolutePath(), null);
            CommandResults results = commandExecutor.exeCommand(null, new ArrayList<>(), new ArrayList<>(), null);
            assertTrue(results.isOk(), results.getErr());
            assertTrue(results.getRes().contains("I have spoken"));
        } finally {
            FileUtils.forceDelete(tmpDir.toFile());
        }
    }
}
