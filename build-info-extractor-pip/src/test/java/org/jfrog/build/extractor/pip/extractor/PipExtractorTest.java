package org.jfrog.build.extractor.pip.extractor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.pip.PipDriver;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

/**
 * Created by Bar Belity on 21/07/2020.
 */
public class PipExtractorTest extends IntegrationTestsBase {

    private static final String PIP_REMOTE_REPO = "build-info-tests-pip-remote";
    private static final String PIP_VIRTUAL_REPO = "build-info-tests-pip-virtual";

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    private ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private String pipEnvVar;
    private PipDriver driver;
    private Map<String, String> env;

    public PipExtractorTest() {
        remoteRepo = PIP_REMOTE_REPO;
        virtualRepo = PIP_VIRTUAL_REPO;
    }

    private enum Project {
        // Test projects.
        SETUPPY("setuppy", "setuppyProject", "setuppy", "jfrog-python-example", ". --no-cache-dir --force-reinstall", 3, true),
        SETUPPYVERBOSE("setupy-verbose", "setuppyProject", "setuppy-verbose", "jfrog-python-example", ". --no-cache-dir --force-reinstall -v", 3, true),
        REQUIREMENTS("requirements", "requirementsProject", "requirements", "jfrog-pip-requirements", "-r requirements.txt --no-cache-dir --force-reinstall", 5, true),
        REQUIREMENTSVERBOSE("requirements-verbose", "requirementsProject", "requirements-verbose", "jfrog-pip-requirements-verbose", "-r requirements.txt -v --no-cache-dir --force-reinstall", 5, false),
        REQUIREMENTSCACHE("requirements-use-cache", "requirementsProject", "requirements-verbose", "jfrog-pip-requirements-usecache", "-r requirements.txt", 5, true);

        private String name;
        private File projectOrigin;
        private String targetDir;
        private String moduleId;
        private String args;
        private int expectedDependencies;
        private boolean cleanAfterExecution;

        Project(String name, String project, String outputFoder, String moduleId, String args, int expectedDependencies, boolean cleanAfterExecution) {
            this.name = name;
            this.projectOrigin = PROJECTS_ROOT.resolve(project).toFile();
            this.targetDir = outputFoder;
            this.moduleId = moduleId;
            this.args = args;
            this.expectedDependencies = expectedDependencies;
            this.cleanAfterExecution = cleanAfterExecution;
        }
    }

    @BeforeClass
    private void setUp() {
        dependenciesClientBuilder = new ArtifactoryDependenciesClientBuilder().setArtifactoryUrl(getUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());

        // Read pip environment path variable.
        pipEnvVar = System.getenv(BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "PIP_ENV");
        if (pipEnvVar == null) {
            fail("Couldn't read pip virtual-environment variable: " + BITESTS_ARTIFACTORY_ENV_VAR_PREFIX + "PIP_ENV");
        }

        // Get env with PATH containing pip-virtual-env and create driver.
        try {
            env = getUpdatedEnvPath();
            driver = new PipDriver("pip", env);
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    @AfterClass
    protected void tearDown() {
        cleanPipEnv(Paths.get("."));
    }

    @DataProvider
    private Object[][] pipInstallProvider() {
        return new Object[][]{
                {Project.SETUPPY},
                {Project.SETUPPYVERBOSE},
                {Project.REQUIREMENTS},
                {Project.REQUIREMENTSVERBOSE},
                {Project.REQUIREMENTSCACHE}
        };
    }

    @Test(dataProvider = "pipInstallProvider")
    private void pipInstallTest(Project project) {
        Path projectDir = null;
        try {
            // Check pip env is clean.
            validateEmptyPipEnv(driver);

            // Copy project files to temp.
            projectDir = TestUtils.createTestProjectTempDir(project.targetDir, project.projectOrigin);

            // Run pip-install.
            PipInstall pipInstall = new PipInstall(dependenciesClientBuilder, virtualRepo, project.args, getLog(),projectDir, env, project.moduleId, getUsername(), getPassword(), null);
            Build build = pipInstall.execute();

            // Validate produced build-info.
            Module module = build.getModules().get(0);
            assertEquals(module.getId(), project.moduleId);
            assertEquals(module.getDependencies().size(), project.expectedDependencies);

            // If should clean env -> clean.
            if (project.cleanAfterExecution) {
                cleanPipEnv(projectDir);
                FileUtils.deleteQuietly(projectDir.toFile());
            }

        } catch (Exception e) {
            // Clean env in case of exception.
            cleanPipEnv(projectDir);
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    private void cleanPipEnv(Path projectDir) {
        try {
            // Run pip freeze.
            String freezeOutput = driver.freeze(projectDir.toFile(), log);
            if (StringUtils.isBlank(freezeOutput)) {
                return;
            }

            // Save freeze output to file.
            Path freezeOutputPath = Paths.get(projectDir.toString(), "pip-freeze.txt");
            Files.write(freezeOutputPath, freezeOutput.getBytes());

            // Delete freezed packages.
            driver.runCommand(projectDir.toFile(), Arrays.asList("uninstall -y -r"), log);
        } catch (IOException | InterruptedException e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    private void validateEmptyPipEnv(PipDriver driver) throws IOException {
        String output = driver.freeze((Paths.get(".")).toFile(), log);
        if (StringUtils.isNotBlank(output)) {
            throw new IOException(String.format("Provided pip virtual-environment contains installed packages: %s\n. Please provide a clean environment", output));
        }
    }

    private Map<String, String> getUpdatedEnvPath() throws IOException {
        // Add virtual env path to 'PATH'.
        String pathValue = System.getenv("PATH");
        //Map<String, String> allEnv = new HashMap<>(System.getenv());
        //String pathValue = allEnv.get("PATH");
        if (StringUtils.isBlank(pathValue)) {
            throw new IOException("Couldn't find PATH variable, failing pip tests");
        }
        String newPathValue;
        if (isWindows()) {
            newPathValue = String.format("%s;%s", pipEnvVar, pathValue);
        } else {
            newPathValue = String.format("%s:%s", pipEnvVar, pathValue);
        }
        //allEnv.replace("PATH", newPathValue);
        Map<String, String> additionalEnvValues = new HashMap<>();
        additionalEnvValues.put("PATH", newPathValue);
        return additionalEnvValues;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

}
