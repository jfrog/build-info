package org.jfrog.build.extractor.npm.extractor;

import org.jfrog.build.api.builder.BuildInfoBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.ci.BuildInfo;
import org.jfrog.build.api.ci.Dependency;
import org.jfrog.build.api.ci.Module;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jfrog.build.extractor.npm.extractor.NpmBuildInfoExtractor.getDependenciesMapFromBuild;
import static org.testng.Assert.assertEquals;

@Test
public class NpmBuildInfoExtractorTest {

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
}
