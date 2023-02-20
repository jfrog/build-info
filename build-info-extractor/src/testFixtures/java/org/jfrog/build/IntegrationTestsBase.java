package org.jfrog.build;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.client.response.GetAllBuildNumbersResponse;
import org.jfrog.build.extractor.util.TestingLog;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prepares the infrastructure resources used by tests.
 * Created by diman on 27/02/2017.
 */
public abstract class IntegrationTestsBase {

    protected static final Log log = new TestingLog();
    protected static final String LOCAL_REPO_PLACEHOLDER = "${LOCAL_REPO}";
    protected static final String LOCAL_REPO2_PLACEHOLDER = "${LOCAL_REPO2}";
    protected static final String VIRTUAL_REPO_PLACEHOLDER = "${VIRTUAL_REPO}";
    protected static final String TEMP_FOLDER_PLACEHOLDER = "${TEMP_FOLDER}";
    protected static final String LOCAL_REPOSITORIES_WILDCARD_PLACEHOLDER = "${LOCAL_REPO1_REPO2}";
    protected static final String UPLOAD_SPEC = "upload.json";
    protected static final String DOWNLOAD_SPEC = "download.json";
    protected static final String EXPECTED = "expected.json";
    protected static final String BITESTS_ENV_VAR_PREFIX = "BITESTS_PLATFORM_";
    private static final String BITESTS_PROPERTIES_PREFIX = "bitests.platform.";
    protected static final String BITESTS_ARTIFACTORY_ENV_VAR_PREFIX = "BITESTS_ARTIFACTORY_";
    protected String localRepo1 = getKeyWithTimestamp("build-info-tests-local");
    protected String localRepo2 = getKeyWithTimestamp("build-info-tests-local2");
    protected String remoteRepo;
    protected String virtualRepo = getKeyWithTimestamp("build-info-tests-virtual");
    protected String localRepositoriesWildcard = "build-info-tests-local*";
    protected ArtifactoryManager artifactoryManager;
    protected ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private String username;
    private String adminToken;
    private String platformUrl;
    private String artifactoryUrl;
    public static final Pattern BUILD_NUMBER_PATTERN = Pattern.compile("^/(\\d+)$");
    public static final long CURRENT_TIME = System.currentTimeMillis();
    StringSubstitutor stringSubstitutor;

    public static Log getLog() {
        return log;
    }

    @BeforeClass
    public void init() throws IOException {
        Properties props = new Properties();
        // This file is not in GitHub. Create your own in src/test/resources or use environment variables.
        InputStream inputStream = this.getClass().getResourceAsStream("/artifactory-bi.properties");

        if (inputStream != null) {
            props.load(inputStream);
            inputStream.close();
        }

        platformUrl = readParam(props, "url", "http://127.0.0.1:8081");
        if (!platformUrl.endsWith("/")) {
            platformUrl += "/";
        }
        artifactoryUrl = platformUrl + "artifactory/";
        username = readParam(props, "username", "admin");
        adminToken = readParam(props, "admin_token", "password");
        artifactoryManager = createArtifactoryManager();
        artifactoryManagerBuilder = createArtifactoryManagerBuilder();

        createStringSubstitutor();
        if (StringUtils.isNotEmpty(localRepo1)) {
            createTestRepo(localRepo1);
        }
        if (StringUtils.isNotEmpty(remoteRepo)) {
            createTestRepo(remoteRepo);
        }
        if (StringUtils.isNotEmpty(virtualRepo)) {
            createTestRepo(virtualRepo);
        }
    }

    @AfterClass
    protected void terminate() throws IOException {
        // Delete the virtual first.
        if (StringUtils.isNotEmpty(virtualRepo)) {
            deleteTestRepo(virtualRepo);
        }
        if (StringUtils.isNotEmpty(remoteRepo)) {
            deleteTestRepo(remoteRepo);
        }
        if (StringUtils.isNotEmpty(localRepo1)) {
            deleteTestRepo(localRepo1);
        }
        artifactoryManager.close();
    }

    private void createStringSubstitutor() {
        Map<String, Object> textParameters = new HashMap<>();
        textParameters.put("LOCAL_REPO_1", localRepo1);
        textParameters.put("REMOTE_REPO", remoteRepo);
        stringSubstitutor = new StringSubstitutor(textParameters);
    }

    protected String readParam(Properties props, String paramName, String defaultValue) {
        String paramValue = null;
        if (props.size() > 0) {
            paramValue = props.getProperty(BITESTS_PROPERTIES_PREFIX + paramName);
        }
        if (StringUtils.isBlank(paramValue)) {
            paramValue = System.getProperty(BITESTS_PROPERTIES_PREFIX + paramName);
        }
        if (StringUtils.isBlank(paramValue)) {
            paramValue = System.getenv(BITESTS_ENV_VAR_PREFIX + paramName.toUpperCase());
        }
        if (StringUtils.isBlank(paramValue)) {
            paramValue = defaultValue;
        }
        return paramValue;
    }

    /**
     * Delete all content from the given repository.
     *
     * @param repoKey - repository key
     */
    protected void deleteContentFromRepo(String repoKey) throws IOException {
        if (!artifactoryManager.isRepositoryExist(repoKey)) {
            return;
        }
        artifactoryManager.deleteRepositoryContent(repoKey);
    }

    /**
     * Create new repository according to the settings.
     *
     * @param repoKey - repository key
     * @throws IOException in case of any connection issues with Artifactory or the repository doesn't exist.
     */
    protected void createTestRepo(String repoKey) throws IOException {
        if (artifactoryManager.isRepositoryExist(repoKey)) {
            return;
        }
        String path = "/integration/settings/" + StringUtils.substringBeforeLast(repoKey, "-") + ".json";
        try (InputStream repoConfigInputStream = this.getClass().getResourceAsStream(path)) {
            if (repoConfigInputStream == null) {
                throw new IOException("Couldn't find repository settings in " + path);
            }
            String json = IOUtils.toString(repoConfigInputStream, StandardCharsets.UTF_8.name());
            artifactoryManager.createRepository(repoKey, stringSubstitutor.replace(json));
        }
    }

    /**
     * Delete repository.
     *
     * @param repo - repository name
     * @throws IOException in case of any I/O error.
     */
    protected void deleteTestRepo(String repo) throws IOException {
        artifactoryManager.deleteRepository(repo);
    }

    /**
     * Read spec file and replace the placeholder test data.
     *
     * @param specFile      - the spec file
     * @param workSpacePath - workspace path
     * @return the File Spec as a string.
     * @throws IOException in case of any I/O error.
     */
    protected String readSpec(File specFile, String workSpacePath) throws IOException {
        String spec = FileUtils.readFileToString(specFile, StandardCharsets.UTF_8);
        spec = StringUtils.replace(spec, LOCAL_REPO_PLACEHOLDER, localRepo1);
        spec = StringUtils.replace(spec, LOCAL_REPO2_PLACEHOLDER, localRepo2);
        spec = StringUtils.replace(spec, VIRTUAL_REPO_PLACEHOLDER, virtualRepo);
        spec = StringUtils.replace(spec, TEMP_FOLDER_PLACEHOLDER, workSpacePath);
        spec = StringUtils.replace(spec, LOCAL_REPOSITORIES_WILDCARD_PLACEHOLDER, localRepositoriesWildcard);
        return StringUtils.replace(spec, "${WORKSPACE}", workSpacePath);
    }

    /**
     * Get repository key with timestamp: key-[timestamp]
     *
     * @param key - The raw key
     * @return key with timestamp
     */
    protected static String getKeyWithTimestamp(String key) {
        return key + "-" + CURRENT_TIME;
    }

    protected void verifyExpected(Expected expected, File workspace) {
        // Verify tempWorkspace exists
        Assert.assertTrue(workspace.exists(), "The path: '" + workspace.getPath() + "' does not exist");
        // Verify expected results
        Collection<File> downloadedFiles = FileUtils.listFiles(workspace, null, true);
        for (String path : expected.getFiles()) {
            File f = new File(workspace, path);
            Assert.assertTrue(downloadedFiles.contains(f), "Missing file: '" + path + "'.");
            downloadedFiles.remove(f);
        }

        for (File f : downloadedFiles) {
            Assert.fail("Unexpected file: '" + f.getPath() + "'.");
        }
    }

    protected String getUsername() {
        return this.username;
    }

    protected String getAdminToken() {
        return adminToken;
    }

    protected String getPlatformUrl() {
        return this.platformUrl;
    }

    protected String getArtifactoryUrl() {
        return this.artifactoryUrl;
    }

    private ArtifactoryManager createArtifactoryManager() {
        return new ArtifactoryManager(artifactoryUrl, username, adminToken, log);
    }

    private ArtifactoryManagerBuilder createArtifactoryManagerBuilder() {
        ArtifactoryManagerBuilder builder = new ArtifactoryManagerBuilder();
        return builder.setServerUrl(artifactoryUrl).setUsername(username).setPassword(adminToken).setLog(log);
    }

    /**
     * Expected inner class for testing purposes.
     * Contains the local files expected to be found after successful download.
     */
    protected static class Expected {
        private List<String> files;

        public List<String> getFiles() {
            return files;
        }

        public void setFiles(List<String> files) {
            this.files = files;
        }
    }

    /**
     * Return true if the build was created more than 24 hours ago.
     *
     * @param buildMatcher - Build regex matcher on BUILD_NUMBER_PATTERN
     * @return true if the Build was created more than 24 hours ago
     */
    private static boolean isOldBuild(Matcher buildMatcher) {
        long repoTimestamp = Long.parseLong(buildMatcher.group(1));
        return TimeUnit.MILLISECONDS.toHours(CURRENT_TIME - repoTimestamp) >= 24;
    }

    public void cleanTestBuilds(String buildName, String buildNumber, String project) throws IOException {
        artifactoryManager.deleteBuilds(buildName, project, true, buildNumber);
        cleanOldBuilds(buildName, project);
    }

    /**
     * Clean up old build runs which have been created more than 24 hours.
     *
     * @param buildName - The build name to be cleaned.
     */
    private void cleanOldBuilds(String buildName, String project) throws IOException {
        // Get build numbers for deletion
        String[] oldBuildNumbers = artifactoryManager.getAllBuildNumbers(buildName, project).buildsNumbers.stream()

                // Get build numbers.
                .map(GetAllBuildNumbersResponse.BuildsNumberDetails::getUri)

                //  Remove duplicates.
                .distinct()

                // Match build number pattern.
                .map(BUILD_NUMBER_PATTERN::matcher)
                .filter(Matcher::matches)

                // Filter build numbers newer than 24 hours.
                .filter(IntegrationTestsBase::isOldBuild)

                // Get build number.
                .map(matcher -> matcher.group(1))
                .toArray(String[]::new);

        if (oldBuildNumbers.length > 0) {
            artifactoryManager.deleteBuilds(buildName, project, true, oldBuildNumbers);
        }
    }
}
