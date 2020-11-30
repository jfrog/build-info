package org.jfrog.build.extractor.npm.extractor;

import org.jfrog.build.api.Build;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.*;

import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.getDependenciesMapFromBuild;
import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.isJsonOutputRequired;
import static org.testng.Assert.assertEquals;

@Test
public class NpmBuildInfoExtractorTest {

    @DataProvider
    private Object[][] isJsonOutputRequiredProvider() {
        return new Object[][]{
                //  Check "--json" possible positions
                {true, "--json"},
                {true, "--json", "arg2"},
                {true, "arg1", "--json"},
                {true, "arg1", "--json", "arg3"},
                //  Check "--json=true" possible positions
                {true, "--json=true"},
                {true, "--json=true", "arg2"},
                {true, "arg1", "--json=true"},
                {true, "arg1", "--json=true", "arg3"},
                //  Check "--json=false" possible positions
                {false, "--json=false"},
                {false, "--json=false", "arg2"},
                {false, "arg1", "--json=false"},
                {false, "arg1", "--json=false", "arg3"},
                //  Check "--json true" possible positions
                {true, "--json", "true"},
                {true, "--json", "true", "arg3"},
                {true, "arg1", "--json", "true"},
                {true, "arg1", "--json", "true", "arg4"},
                //  Check "--json false" possible positions
                {false, "--json", "false"},
                {false, "--json", "false", "arg3"},
                {false, "arg1", "--json", "false"},
                {false, "arg1", "--json", "false", "arg4"},
        };
    }

    @Test(dataProvider = "isJsonOutputRequiredProvider")
    public void isJsonOutputRequiredTest(boolean expectedResult, String[] installationArgs) {
        assertEquals(isJsonOutputRequired(Lists.newArrayList(installationArgs)), expectedResult);
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
    public void getDependenciesMapFromBuildTest(Build build, Map<String, Dependency> expected) {
        Map<String, Dependency> actual = getDependenciesMapFromBuild(build);
        assertEquals(actual, expected);
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
}
