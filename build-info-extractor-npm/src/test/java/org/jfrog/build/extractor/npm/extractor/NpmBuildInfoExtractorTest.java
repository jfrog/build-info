package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.IntegrationTestsBase;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Dependency;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.npm.NpmDriver;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.*;

import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.getDependenciesMapFromBuild;

@Test
public class NpmBuildInfoExtractorTest extends IntegrationTestsBase {

    private static final String TEST_SPACE = "npm_test_space";
    private static final File tempWorkspace = new File(System.getProperty("java.io.tmpdir"), TEST_SPACE);
    private static final Path PROJECTS_ROOT = Paths.get(".").toAbsolutePath().normalize().resolve(Paths.get("src", "test", "resources", "org", "jfrog", "build", "extractor"));


    private static final String NPM_LOCAL_REPO = "build-info-tests-npm-local";
    private static final Set<String> DEV_SCOPE = Stream.of("dev").collect(Collectors.toSet());
    private static final Set<String> PROD_SCOPE = Stream.of("prod").collect(Collectors.toSet());
    private static final Set<String> DEV_PROD_SCOPE = Stream.of("prod", "dev").collect(Collectors.toSet());

    public NpmBuildInfoExtractorTest() {
        localRepo1 = getKeyWithTimestamp(NPM_LOCAL_REPO);
        remoteRepo = "";
        virtualRepo = "";
    }

    @AfterMethod
    protected void cleanup() throws IOException {
        FileUtils.deleteDirectory(tempWorkspace);
    }

    @BeforeMethod
    protected void init2() throws IOException {
        FileUtils.forceMkdir(tempWorkspace);
    }
    @DataProvider
    private Object[][] getDependenciesMapFromBuildProvider() {
        return new Object[][]{
                {
                        new BuildInfoBuilder("npm-dependencies-map-test1").number("1").started("start1")
                                .addModule(createTestModule("module-1", Arrays.asList(dependenciesArray[0], dependenciesArray[1], dependenciesArray[2])))
                                .addModule(createTestModule("module-2", Arrays.asList(dependenciesArray[3], dependenciesArray[4])))
                                .build(),
                        new HashMap<String, Dependency>() {{
                            put("mod1dep1:1.1.0", dependenciesArray[0]);
                            put("mod1dep2:1.2.0", dependenciesArray[1]);
                            put("mod1dep3:1.3.0", dependenciesArray[2]);
                            put("mod2dep1:2.1.0", dependenciesArray[3]);
                            put("mod2dep2:2.2.0", dependenciesArray[4]);
                        }}
                },
                {
                        new BuildInfoBuilder("npm-dependencies-map-test2").number("2").started("start2")
                                .addModule(createTestModule("module-1", Arrays.asList(dependenciesArray[0], dependenciesArray[1], dependenciesArray[2], dependenciesArray[0])))
                                .addModule(createTestModule("module-2", Arrays.asList(dependenciesArray[3], dependenciesArray[1], dependenciesArray[4])))
                                .build(),
                        new HashMap<String, Dependency>() {{
                            put("mod1dep1:1.1.0", dependenciesArray[0]);
                            put("mod1dep2:1.2.0", dependenciesArray[1]);
                            put("mod1dep3:1.3.0", dependenciesArray[2]);
                            put("mod2dep1:2.1.0", dependenciesArray[3]);
                            put("mod2dep2:2.2.0", dependenciesArray[4]);
                        }}
                },
                {
                        new BuildInfoBuilder("npm-dependencies-map-test3").number("3").started("start3")
                                .addModule(createTestModule("module-1", Collections.singletonList(dependenciesArray[0])))
                                .addModule(createTestModule("module-2", Collections.singletonList(dependenciesArray[0])))
                                .addModule(createTestModule("module-3", Collections.singletonList(dependenciesArray[0])))
                                .build(),
                        new HashMap<String, Dependency>() {{
                            put("mod1dep1:1.1.0", dependenciesArray[0]);
                        }}
                },
                {
                        new BuildInfoBuilder("npm-dependencies-map-test4").number("4").started("start4")
                                .addModule(createTestModule("module-1", Collections.singletonList(dependenciesArray[3])))
                                .addModule(createTestModule("module-2", Arrays.asList(
                                        new DependencyBuilder().id("mod2dep1:2.1.0").sha1("sha1-mod2dep1").md5("md5-mod2dep1").build(),
                                        dependenciesArray[1], dependenciesArray[4])))
                                .build(),
                        new HashMap<String, Dependency>() {{
                            put("mod1dep2:1.2.0", dependenciesArray[1]);
                            put("mod2dep1:2.1.0", dependenciesArray[3]);
                            put("mod2dep2:2.2.0", dependenciesArray[4]);
                        }}
                }
        };
    }

    @Test(dataProvider = "getDependenciesMapFromBuildProvider")
    public void getDependenciesMapFromBuildTest(BuildInfo buildInfo, Map<String, Dependency> expected) {
        Map<String, Dependency> actual = getDependenciesMapFromBuild(buildInfo);
        assertEquals(actual, expected);
    }

    @DataProvider
    private Object[][] setTypeRestrictionProvider() {
        return new Object[][]{
                {NpmBuildInfoExtractor.TypeRestriction.PROD_ONLY, new String[]{"production", "true"}},
                {NpmBuildInfoExtractor.TypeRestriction.PROD_ONLY, new String[]{"only", "prod"}},
                {NpmBuildInfoExtractor.TypeRestriction.PROD_ONLY, new String[]{"only", "production"}},
                {NpmBuildInfoExtractor.TypeRestriction.DEV_ONLY, new String[]{"only", "dev"}},
                {NpmBuildInfoExtractor.TypeRestriction.DEV_ONLY, new String[]{"only", "development"}},
                {NpmBuildInfoExtractor.TypeRestriction.PROD_ONLY, new String[]{"omit", "[\"dev\"]"}, new String[]{"k1", "v1"}, new String[]{"dev", "true"}},
                {NpmBuildInfoExtractor.TypeRestriction.ALL, new String[]{"omit", "[\"abc\"]"}, new String[]{"dev", "true"}},
                {NpmBuildInfoExtractor.TypeRestriction.ALL, new String[]{"only", "dev"}, new String[]{"omit", "[\"abc\"]"}},
                {NpmBuildInfoExtractor.TypeRestriction.PROD_ONLY, new String[]{"dev", "true"}, new String[]{"omit", "[\"dev\"]"}},
                {NpmBuildInfoExtractor.TypeRestriction.DEFAULT_RESTRICTION, new String[]{"kuku", "true"}}
        };
    }

    @Test(dataProvider = "setTypeRestrictionProvider")
    public void setTypeRestrictionTest(NpmBuildInfoExtractor.TypeRestriction expected, String[][] confs) {
        NpmBuildInfoExtractor extractor = new NpmBuildInfoExtractor(null, null, null, null, null, null);

        for (String[] conf : confs) {
            extractor.setTypeRestriction(conf[0], conf[1]);
        }

        assertEquals(extractor.getTypeRestriction(), expected);
    }

    private Module createTestModule(String id, List<Dependency> dependencies) {
        return new ModuleBuilder().id(id)
                .dependencies(dependencies)
                .build();
    }

    private final Dependency[] dependenciesArray = new Dependency[]{
            new DependencyBuilder().id("mod1dep1:1.1.0").sha1("sha1-mod1dep1").md5("md5-mod1dep1").build(),
            new DependencyBuilder().id("mod1dep2:1.2.0").sha1("sha1-mod1dep2").md5("md5-mod1dep2").build(),
            new DependencyBuilder().id("mod1dep3:1.3.0").sha1("sha1-mod1dep3").md5("md5-mod1dep3").build(),
            new DependencyBuilder().id("mod2dep1:2.1.0").sha1("sha1-mod2dep1").md5("md5-mod2dep1").build(),
            new DependencyBuilder().id("mod2dep2:2.2.0").sha1("sha1-mod2dep2").md5("md5-mod2dep2").build()
    };

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
        runNpmTest(project, expectedDependencies, args, packageJsonPath);
    }

    private void runNpmTest(Project project, Dependency[] expectedDependencies, String args, boolean packageJsonPath) {
        args += " --verbose --no-audit";
        Path projectDir = null;
        boolean isNpmCi = true;
        try {
            // Prepare.
            projectDir = createProjectDir(project);
            Path path = packageJsonPath ? projectDir.resolve("package.json") : projectDir;
            if (isNpmCi) {
                // Run npm install to generate package-lock.json file.
                new NpmInstallCi(artifactoryManagerBuilder, localRepo1, args, log, path, null, null, null, false, null).execute();
            }

            NpmDriver driver = new NpmDriver(null);
            List<String> commandArgs = StringUtils.isBlank(args) ? new ArrayList<>() : Arrays.asList(args.trim().split("\\s+"));
            NpmProject proj = new NpmProject(commandArgs, localRepo1, path, isNpmCi);
            // Execute command.
            
            NpmBuildInfoExtractor buildExtractor = new NpmBuildInfoExtractor(artifactoryManagerBuilder, driver, log, null, null,null);
            BuildInfo buildInfo = buildExtractor.extract(proj);

            // Validate.
            assertEquals(buildInfo.getModules().size(), 1);
            Module module = buildInfo.getModules().get(0);
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

    private Path createProjectDir(Project project) throws IOException {
        File projectDir = Files.createTempDirectory(project.targetDir).toFile();
        FileUtils.copyDirectory(project.projectOrigin, projectDir);
        return projectDir.toPath();
    }

}
