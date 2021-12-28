package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.io.FileUtils;
import org.jfrog.build.extractor.ci.Vcs;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.executor.CommandExecutor;
import org.jfrog.build.extractor.executor.CommandResults;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
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
        Assert.assertEquals(vcs.getBranch(), getGitBranchWithExecutor(curDir, testLog));
        Assert.assertEquals(vcs.getMessage(), getGitMessageWithExecutor(curDir, testLog));
    }

    private String getGitFieldWithExecutor(File execDir, Log log, List<String> args) throws IOException, InterruptedException {
        CommandExecutor executor = new CommandExecutor("git", null);
        CommandResults res = executor.exeCommand(execDir, args, null, log);
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

    private String getGitBranchWithExecutor(File execDir, Log log) throws IOException, InterruptedException {
        List<String> args = new ArrayList<>();
        args.add("rev-parse");
        args.add("--abbrev-ref");
        args.add("HEAD");
        return getGitFieldWithExecutor(execDir, log, args);
    }

    private String getGitMessageWithExecutor(File execDir, Log log) throws IOException, InterruptedException {
        // git log -1 --pretty=%B
        List<String> args = new ArrayList<>();
        args.add("log");
        args.add("-1");
        args.add("--pretty=%B");
        return getGitFieldWithExecutor(execDir, log, args);
    }

    @Test
    private void testExtractVcsWithSubmodule() throws IOException, URISyntaxException {
        String parentDotGitResource = "git_submodule_parent_.git_suffix";
        String submoduleDotGitResource = "git_submodule_.git_suffix";
        File testResourcesPath = new File(this.getClass().getResource("/gitutils").toURI()).getCanonicalFile();
        File parentDotGitFile = new File(testResourcesPath, ".git");
        String submoduleDotGitFilePath = testResourcesPath.toString() + File.separator + "subDir" + File.separator +
                "subModule" + File.separator + ".git";
        File submoduleDotGitFile = new File(submoduleDotGitFilePath);

        try {
            // Copy the parent folder and create .git.
            FileUtils.copyDirectory(new File(testResourcesPath, parentDotGitResource), parentDotGitFile);

            // Copy the submodule .git file to the correct path.
            FileUtils.copyFile(new File(testResourcesPath, submoduleDotGitResource), submoduleDotGitFile);

            // Get VCS info.
            Log testLog = new TestingLog();
            Vcs vcs = GitUtils.extractVcs((new File(submoduleDotGitFilePath)).getAbsoluteFile(), testLog);

            // Validate.
            Assert.assertNotNull(vcs);
            Assert.assertEquals(vcs.getUrl(), "https://github.com/jfrog/subModule.git");
            Assert.assertEquals(vcs.getRevision(), "thisIsADummyRevision");
        } finally {
            // Cleanup.
            FileUtils.forceDelete(submoduleDotGitFile);
            FileUtils.deleteDirectory(parentDotGitFile);
        }
    }
}
