package org.jfrog.build.extractor.npm.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
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
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

/**
 * @author Yahav Itzhak
 */
@Test
public class NpmExtractorTest extends IntegrationTestsBase {

    private static final String NPM_LOCAL_REPO = "build-info-tests-npm-local";
    private static final String NPM_REMOTE_REPO = "build-info-tests-npm-remote";
    private static final String NPM_VIRTUAL_REPO = "build-info-tests-npm-virtual";
    private static final Set<String> DEV_SCOPE = Stream.of("dev").collect(Collectors.toSet());
    private static final Set<String> PROD_SCOPE = Stream.of("prod").collect(Collectors.toSet());
    private static final Set<String> DEV_PROD_SCOPE = Stream.of("prod","dev").collect(Collectors.toSet());

    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));

    private ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private DependenciesDownloaderHelper downloaderHelper;

    public NpmExtractorTest() {
        localRepo1 = NPM_LOCAL_REPO;
        remoteRepo = NPM_REMOTE_REPO;
        virtualRepo = NPM_VIRTUAL_REPO;
    }

    private enum Project {
        // Dependencies
        ASGARD("asgard", "jfrog-asgard", "jfrog-asgard", "2.0.0", "a1fc28aa8733a161fa92d03379b71468d19292cd", "2fb7c420d2119831bc38559138d3444e"),
        MIDGARD("midgard", "jfrog-midgard", "jfrog-midgard", "1.0.0", "547b8c7bb019863cc26438ef36e9b2d33668a626", "82f1558593727a7c89fb0b91859dab26"),
        ALFHEIM("alfheim", "jfrog-alfheim", "jfrog-alfheim", "3.5.2", "f5592b523d2693649a94bbc2377cc653607a4053", "93e19985bb1c7c815abef052b67be244"),
        SVARTALFHEIM("svartalfheim", "jfrog-svartalfheim", "jfrog-svartalfheim", "0.5.0", "473a5e001c67d716b8c9993245bd0ba2010c7374", "b1678118e32908b8e57f26fef1a23473"),

        // Test projects
        A("a", "NpmExtractorTest Project A", "package-name1", "v0.0.1", "", ""),
        B("b", "NpmExtractorTest-Project-B", "package-name2", "0.0.2", "", ""),
        C("c", "NpmExtractorTestProjectC", "package-name3", "=0.0.3", "", "");

        private final File projectOrigin;
        private final String targetDir;
        private final String name;
        private final String version;
        private final String sha1;
        private final String md5;

        Project(String sourceDir, String targetDir, String name, String version, String sha1, String md5) {
            this.projectOrigin = PROJECTS_ROOT.resolve(sourceDir).toFile();
            this.targetDir = targetDir;
            this.name = name;
            this.version = version;
            this.sha1 = sha1;
            this.md5 = md5;
        }

        private String getModuleId() {
            return String.format("%s:%s", name, version);
        }

        private String getPackedFileName() {
            return String.format("%s-%s.tgz", name, version);
        }

        private String getDependencyId() {
            return String.format("%s:%s", name, version);
        }

        private Dependency toDependency(String[][] requestedBy, Set<String> scope) {
            return new DependencyBuilder().id(getDependencyId())
                    .sha1(sha1)
                    .md5(md5)
                    .scopes(scope)
                    .requestedBy(requestedBy)
                    .build();
        }

        private String getRemotePath() {
            return String.format("%s/-", name);
        }

        private String getTargetPath() {
            return String.format("%s/%s", getRemotePath(), getPackedFileName());
        }
    }

    @BeforeClass
    private void setUp() throws IOException {
        artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setServerUrl(getArtifactoryUrl()).setUsername(getUsername()).setPassword(getPassword()).setLog(getLog());
        downloaderHelper = new DependenciesDownloaderHelper(artifactoryManager, ".", log);
        deployTestDependencies(Project.ASGARD, Project.MIDGARD, Project.ALFHEIM, Project.SVARTALFHEIM);
    }

    private void deployTestDependencies(Project... projects) throws IOException {
        for (Project project : projects) {
            DeployDetails deployDetails = new DeployDetails.Builder()
                    .file(project.projectOrigin.toPath().resolve(project.getPackedFileName()).toFile())
                    .targetRepository(localRepo1)
                    .artifactPath(project.getTargetPath())
                    .packageType(DeployDetails.PackageType.NPM)
                    .build();
            artifactoryManager.upload(deployDetails);
        }
    }


    @DataProvider
    private Object[][] npmInstallProvider() {
        Dependency[] expectedDepsStep1 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"package-name1:v0.0.1"}}, PROD_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name1:v0.0.1"}}, PROD_SCOPE)};
        Dependency[] expectedDepsStep2 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "@jscope/package-name2:0.0.2"}}, DEV_SCOPE), Project.MIDGARD.toDependency(new String[][]{{"@jscope/package-name2:0.0.2"}}, DEV_SCOPE), Project.ALFHEIM.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "@jscope/package-name2:0.0.2"}}, DEV_SCOPE)};
        Dependency[] expectedDepsStep3 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "package-name3:=0.0.3"}, {"package-name3:=0.0.3"}}, DEV_PROD_SCOPE), Project.MIDGARD.toDependency(new String[][]{{"package-name3:=0.0.3"}}, DEV_SCOPE), Project.ALFHEIM.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "package-name3:=0.0.3"}}, DEV_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE)};
        Dependency[] expectedDepsStep4 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE)};

        return new Object[][]{
                {Project.A, expectedDepsStep1, "", true},
                {Project.A, expectedDepsStep1, "--only=prod", true},
                {Project.B, expectedDepsStep2, "", false},
                {Project.B, new Dependency[]{}, "--production", false},
                {Project.C, expectedDepsStep3, "", true},
                {Project.C, expectedDepsStep4, "--only=production", true},
                {Project.C, expectedDepsStep3, "--verbose", true}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmInstallProvider")
    public void npmInstallTest(Project project, Dependency[] expectedDependencies, String args, boolean packageJsonPath) {
        runNpmTest(project, expectedDependencies, args, packageJsonPath, false);
    }

    @DataProvider
    private Object[][] npmCiProvider() {
        Dependency[] expectedDepsStep1 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"package-name1:v0.0.1"}}, PROD_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name1:v0.0.1"}}, PROD_SCOPE)};
        Dependency[] expectedDepsStep2 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "@jscope/package-name2:0.0.2"}}, DEV_SCOPE), Project.MIDGARD.toDependency(new String[][]{{"@jscope/package-name2:0.0.2"}}, DEV_SCOPE), Project.ALFHEIM.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "@jscope/package-name2:0.0.2"}}, DEV_SCOPE)};
        Dependency[] expectedDepsStep3 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "package-name3:=0.0.3"}, {"package-name3:=0.0.3"}}, DEV_PROD_SCOPE), Project.MIDGARD.toDependency(new String[][]{{"package-name3:=0.0.3"}}, DEV_SCOPE), Project.ALFHEIM.toDependency(new String[][]{{"jfrog-midgard:1.0.0", "package-name3:=0.0.3"}}, DEV_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE)};
        Dependency[] expectedDepsStep4 = new Dependency[]{Project.ASGARD.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE), Project.SVARTALFHEIM.toDependency(new String[][]{{"package-name3:=0.0.3"}}, PROD_SCOPE)};
        return new Object[][]{
                {Project.A, expectedDepsStep1, "", true},
                {Project.B, expectedDepsStep2, "", true},
                {Project.B, new Dependency[]{}, "--production", false},
                {Project.C, expectedDepsStep3, "", true},
                {Project.C, expectedDepsStep4, "--only=production", true}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmCiProvider")
    public void npmCiTest(Project project, Dependency[] expectedDependencies, String args, boolean packageJsonPath) {
        runNpmTest(project, expectedDependencies, args, packageJsonPath, true);
    }

    private void runNpmTest(Project project, Dependency[] expectedDependencies, String args, boolean packageJsonPath, boolean isNpmCi) {
        Path projectDir = null;
        try {
            // Prepare.
            projectDir = createProjectDir(project);
            Path path = packageJsonPath ? projectDir.resolve("package.json") : projectDir;
            if (isNpmCi) {
                // Run npm install to generate package-lock.json file.
                new NpmInstallCi(artifactoryManagerBuilder, virtualRepo, args, log, path, null, null, null, false, null).execute();
            }

            // Execute command.
            NpmInstallCi buildExecutor;
            if (isNpmCi) {
                buildExecutor = new NpmInstallCi(artifactoryManagerBuilder, virtualRepo, args, log, path, null, null, null, true, null);
            } else {
                buildExecutor = new NpmInstallCi(artifactoryManagerBuilder, virtualRepo, args, log, path, null, null, null, false, null);
            }
            Build build = buildExecutor.execute();

            // Validate.
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);
            assertEquals(module.getType(), "npm");
            assertEquals(module.getId(), project.getModuleId());
            assertEqualsNoOrder(module.getDependencies().toArray(), expectedDependencies);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    @DataProvider
    private Object[][] npmPublishProvider() {
        return new Object[][]{
                {Project.A, ArrayListMultimap.create(), Project.A.getTargetPath(), ""},
                {Project.A, ArrayListMultimap.create(ImmutableMultimap.of("a", "b")), Project.A.getTargetPath(), ""},
                {Project.B, ArrayListMultimap.create(), Project.B.getTargetPath(), Project.B.getPackedFileName()},
                {Project.B, ArrayListMultimap.create(ImmutableMultimap.of("a", "b", "c", "d")), Project.B.getTargetPath(), Project.B.getPackedFileName()},
                {Project.C, ArrayListMultimap.create(), Project.C.getTargetPath(), ""},
                {Project.C, ArrayListMultimap.create(ImmutableMultimap.of("a", "b", "a", "d")), Project.C.getTargetPath(), ""}
        };
    }

    @SuppressWarnings("unused")
    @Test(dataProvider = "npmPublishProvider")
    public void npmPublishTest(Project project, ArrayListMultimap<String, String> props, String targetPath, String packageName) {
        Path projectDir = null;
        try {
            // Run npm publish
            projectDir = createProjectDir(project);
            Path path = StringUtils.isNotBlank(packageName) ? projectDir.resolve(packageName) : projectDir;
            NpmPublish npmPublish = new NpmPublish(artifactoryManagerBuilder, props, path, virtualRepo, log, null, null);
            Build build = npmPublish.execute();
            assertEquals(build.getModules().size(), 1);
            Module module = build.getModules().get(0);

            // Check correctness of the module and the artifact
            assertEquals(module.getType(), "npm");
            assertEquals(module.getId(), project.getModuleId());
            assertEquals(module.getRepository(), virtualRepo);
            assertEquals(module.getArtifacts().size(), 1);
            assertEquals(module.getArtifacts().get(0).getName(), project.getModuleId());
            assertEquals(module.getArtifacts().get(0).getRemotePath(), project.getRemotePath());

            // DownloadBase the artifact and check for its properties
            StringJoiner propertiesBuilder = new StringJoiner(";");
            props.entries().forEach(property -> propertiesBuilder.add(property.getKey() + "=" + property.getValue()));
            FileSpec fileSpec = new FileSpec();
            fileSpec.setProps(propertiesBuilder.toString());
            fileSpec.setPattern(localRepo1 + "/" + targetPath);
            fileSpec.setTarget(projectDir.toString());

            Spec spec = new Spec();
            spec.setFiles(new FileSpec[]{fileSpec});
            assertEquals(downloaderHelper.downloadDependencies(spec).size(), 1);
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        } finally {
            if (projectDir != null) {
                FileUtils.deleteQuietly(projectDir.toFile());
            }
        }
    }

    private Path createProjectDir(Project project) throws IOException {
        File projectDir = Files.createTempDirectory(project.targetDir).toFile();
        FileUtils.copyDirectory(project.projectOrigin, projectDir);
        return projectDir.toPath();
    }
}
