package org.jfrog.build.extractor.npm.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;
import org.jfrog.build.extractor.clientConfiguration.util.spec.Spec;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class NpmExtractorTest extends IntegrationTestsBase {

    private static final String NPM_LOCAL_REPO = "build-info-tests-npm-local";
    private static final String NPM_REMOTE_REPO = "build-info-tests-npm-remote";
    private static final String NPM_VIRTUAL_REPO = "build-info-tests-npm-virtual";

    private static final Path PROJECTS_PATH = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));
    private static final File PROJECT_A_SOURCE = PROJECTS_PATH.resolve("a").toFile();
    private static final File PROJECT_B_SOURCE = PROJECTS_PATH.resolve("b").toFile();
    private static final File PROJECT_C_SOURCE = PROJECTS_PATH.resolve("c").toFile();

    private static final String PACKAGE_A_NAME = "package-name1:0.0.1";
    private static final String PACKAGE_A_TARGET_PATH = "package-name1/-/package-name1-0.0.1.tgz";
    private static final Set<String> PROJECT_A_DEPENDENCIES = Sets.newHashSet("debug-3.1.0.tgz", "ms-2.0.0.tgz");
    private static final String PACKAGE_B_NAME = "package-name2:0.0.2";
    private static final String PACKAGE_B_TARGET_PATH = "package-name2/-/package-name2-0.0.2.tgz";
    private static final Set<String> PROJECT_B_DEPENDENCIES = Sets.newHashSet("debug-3.1.0.tgz", "ms-2.0.0.tgz", "fresh-0.1.0.tgz", "mime-1.2.6.tgz", "range-parser-0.0.4.tgz", "send-0.1.0.tgz");
    private static final String PACKAGE_C_NAME = "package-name3:0.0.3";
    private static final String PACKAGE_C_TARGET_PATH = "package-name3/-/package-name3-0.0.3.tgz";
    private static final Set<String> PROJECT_C_DEPENDENCIES = Sets.union(PROJECT_A_DEPENDENCIES, PROJECT_B_DEPENDENCIES);

    private DependenciesDownloaderHelper downloaderHelper;

    public NpmExtractorTest() {
        localRepo = NPM_LOCAL_REPO;
        remoteRepo = NPM_REMOTE_REPO;
        virtualRepo = NPM_VIRTUAL_REPO;
    }

    private enum PROJECTS {
        A("NpmExtractorTest Project A", PROJECT_A_SOURCE),
        B("NpmExtractorTest-Project-B", PROJECT_B_SOURCE),
        C("NpmExtractorTestProjectC", PROJECT_C_SOURCE);

        private String projectName;
        private File projectOrigin;

        PROJECTS(String projectName, File projectOrigin) {
            this.projectName = projectName;
            this.projectOrigin = projectOrigin;
        }

        public String getProjectName() {
            return this.projectName;
        }

        public File getProjectOrigin() {
            return this.projectOrigin;
        }
    }

    @BeforeClass
    private void setUp() {
        downloaderHelper = new DependenciesDownloaderHelper(dependenciesClient, ".", log);
    }

    @DataProvider
    private Object[][] npmInstallProvider(){
        return new Object[][] {
                {PROJECTS.A, PACKAGE_A_NAME, PROJECT_A_DEPENDENCIES, ""},
                {PROJECTS.A, "development:" + PACKAGE_A_NAME, Collections.emptySet(), "--only=dev"},
                {PROJECTS.A, "production:" + PACKAGE_A_NAME, PROJECT_A_DEPENDENCIES, "--only=prod"},
                {PROJECTS.B, PACKAGE_B_NAME, PROJECT_B_DEPENDENCIES, ""},
                {PROJECTS.B, "development:" + PACKAGE_B_NAME, PROJECT_B_DEPENDENCIES, "--only=dev"},
                {PROJECTS.B, "production:" + PACKAGE_B_NAME, Collections.emptySet(), "--production"},
                {PROJECTS.C, PACKAGE_C_NAME, PROJECT_C_DEPENDENCIES, ""},
                {PROJECTS.C, "development:" + PACKAGE_C_NAME, PROJECT_B_DEPENDENCIES, "--only=development"},
                {PROJECTS.C, "production:" + PACKAGE_C_NAME, PROJECT_A_DEPENDENCIES, "--only=production"}
        };
    }

    @DataProvider
    private Object[][] npmPublishProvider(){
        return new Object[][]{
                {PROJECTS.A, ArrayListMultimap.create(), PACKAGE_A_NAME, PACKAGE_A_TARGET_PATH, ""},
                {PROJECTS.A, ArrayListMultimap.create(ImmutableMultimap.<String, String>builder().put("a", "b").build()), PACKAGE_A_NAME, PACKAGE_A_TARGET_PATH, ""},
                {PROJECTS.B, ArrayListMultimap.create(), PACKAGE_B_NAME, PACKAGE_B_TARGET_PATH, ""},
                {PROJECTS.B, ArrayListMultimap.create(ImmutableMultimap.<String, String>builder().put("a", "b").put("c", "d").build()), PACKAGE_B_NAME, PACKAGE_B_TARGET_PATH, ""},
                {PROJECTS.C, ArrayListMultimap.create(), PACKAGE_C_NAME, PACKAGE_C_TARGET_PATH, ""},
                {PROJECTS.C, ArrayListMultimap.create(ImmutableMultimap.<String, String>builder().put("a", "b").put("a", "d").build()), PACKAGE_C_NAME, PACKAGE_C_TARGET_PATH, ""}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmInstallProvider")
    private void npmInstallTest(PROJECTS project, String expectedPackageName, Set<String> expectedDependencies, String args) {
        File projectDir = null;
        try {
            // Run npm install
            projectDir = createProjectDir(project);
            NpmInstall npmInstall = new NpmInstall(dependenciesClient, virtualRepo, args, null, log, projectDir);
            Module module = npmInstall.execute();

            // Check that the module currect
            assertEquals(module.getId(), expectedPackageName);
            Set<String> moduleDependencies = module.getDependencies().stream().map(Dependency::getId).collect(Collectors.toSet());
            assertEquals(moduleDependencies, expectedDependencies);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            FileUtils.deleteQuietly(projectDir);
        }
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmPublishProvider")
    private void npmPublishTest(PROJECTS project, ArrayListMultimap<String, String> props, String expectedPackageName, String targetPath, String args) {
        File projectDir = null;
        try {
            // Run npm publish
            projectDir = createProjectDir(project);
            NpmPublish npmPublish = new NpmPublish(buildInfoClient, props, null, projectDir, virtualRepo, args);
            Module module = npmPublish.execute();

            // Check that the module correct
            assertEquals(module.getId(), expectedPackageName);
            assertEquals(module.getArtifacts().size(), 1);
            assertEquals(module.getArtifacts().get(0).getName(), expectedPackageName);

            // Download the artifact and check for its properties
            StringJoiner propertiesBuilder = new StringJoiner(";");
            props.entries().forEach(property -> propertiesBuilder.add(property.getKey() + "=" + property.getValue()));
            FileSpec fileSpec = new FileSpec();
            fileSpec.setProps(propertiesBuilder.toString());
            fileSpec.setPattern(localRepo + "/" + targetPath);
            fileSpec.setTarget(Paths.get(projectDir.getPath(), "download", "/").toString());
            Spec spec = new Spec();
            spec.setFiles(new FileSpec[]{fileSpec});
            assertEquals(downloaderHelper.downloadDependencies(spec).size(), 1);
        } catch (Exception e) {
            fail(e.getMessage());
        } finally {
            FileUtils.deleteQuietly(projectDir);
        }
    }

    private File createProjectDir(PROJECTS project) throws IOException {
        File projectFile = Files.createTempDirectory(project.getProjectName()).toFile();
        FileUtils.copyDirectory(project.getProjectOrigin(), projectFile);
        return projectFile;
    }
}
