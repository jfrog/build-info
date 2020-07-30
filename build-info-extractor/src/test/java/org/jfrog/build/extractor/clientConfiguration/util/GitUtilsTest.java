package org.jfrog.build.extractor.clientConfiguration.util;

import org.jfrog.build.api.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GitUtilsTest {

    /**
     * Tests extracting Vcs details manually by comparing results to those received from the git executable
     */
    @Test
    private void testReadGitConfig() throws IOException, InterruptedException {
        File curDir = new File("").getAbsoluteFile();
        Log testLog = new TestingLog();
        Vcs vcs = GitUtils.extractVcs(curDir, testLog);

        Assert.assertNotNull(vcs);
        Assert.assertEquals(vcs.getUrl(), getGitUrlWithExecutor(curDir, testLog));
        Assert.assertEquals(vcs.getRevision(), getGitRevisionWithExecutor(curDir, testLog));
    }

    private String getGitFieldWithExecutor(File execDir, Log log, List<String> args) throws IOException, InterruptedException {
        CommandExecutor executor = new CommandExecutor("git", null);
        CommandResults res = executor.exeCommand(execDir, args, log);
        Assert.assertTrue(res.isOk());
        return res.getRes().trim();
    }

    private String getGitUrlWithExecutor(File execDir, Log log) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("config");
        args.add("--get");
        args.add("remote.origin.url");
        return getGitFieldWithExecutor(execDir, log, args);
    }

    private String getGitRevisionWithExecutor(File execDir, Log log) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("show");
        args.add("-s");
        args.add("--format=%H");
        args.add("HEAD");
        return getGitFieldWithExecutor(execDir, log, args);
    }
}
