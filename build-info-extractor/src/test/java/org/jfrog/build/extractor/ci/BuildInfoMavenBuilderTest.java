package org.jfrog.build.extractor.ci;

import org.jfrog.build.api.Build;
import org.jfrog.build.extractor.builder.ArtifactBuilder;
import org.jfrog.build.extractor.builder.BuildInfoBuilder;
import org.jfrog.build.extractor.builder.BuildInfoMavenBuilder;
import org.jfrog.build.extractor.builder.DependencyBuilder;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.PromotionStatus;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Author: Shay Yaakov
 * Date: 8/25/12
 * Time: 10:55 PM
 */
@Test
public class BuildInfoMavenBuilderTest {

    public static final String SHA1 = "e4e264c711ae7ab54f26542f0dd09a43b93fa12c";
    public static final String SHA2 = "yyyy23029162f3b2dc51f512cb64bce8cb6913ed6e540f23ec567d898f60yyyy";
    public static final String MD5 = "d9303a42c66c2824fd6ba0f75e335294";
    public static final String DEPLOY_REPO = "repo";

    /**
     * Validates the build values when using the defaults
     */
    public void testDefaultBuild() {
        BuildInfo buildInfo = new BuildInfoBuilder("test").number("4").started("test").build();
        assertEquals(buildInfo.getVersion(), "1.0.1", "Unexpected default build version.");
        assertEquals(buildInfo.getNumber(), "4", "Unexpected default build number.");

        assertNull(buildInfo.getAgent(), "Default build agent should be null.");

        assertEquals(buildInfo.getDurationMillis(), 0, "Default build duration millis should be zero.");
        assertNull(buildInfo.getPrincipal(), "Default build principal should be null.");
        assertNull(buildInfo.getArtifactoryPrincipal(), "Default build artifactory principal should be null.");
        assertNull(buildInfo.getArtifactoryPluginVersion(), "Default build ArtifactoryPluginVersion should be null.");
        assertNull(buildInfo.getUrl(), "Default build URL should be null.");
        assertNull(buildInfo.getParentName(), "Default build parent build name should be null.");
        assertNull(buildInfo.getParentNumber(), "Default build parent build number should be null.");
        assertNull(buildInfo.getModules(), "Default build modules should be null.");
        assertNull(buildInfo.getProperties(), "Default properties should be null.");
        assertEquals(buildInfo.getVcs(), new ArrayList<Vcs>(), "Default vcs revision should be null.");
    }

    /**
     * Validates the build values after using the builder setters
     */
    public void testBuilderSetters() {
        String version = "1.2.0";
        String name = "moo";
        String number = "15";
        Agent agent = new Agent("pop", "1.6");
        BuildAgent buildAgent = new BuildAgent("rock", "2.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String artifactoryPluginVersion = "2.3.1";
        String url = "mitz";
        String parentName = "pooh";
        String parentNumber = "5";
        List<Module> modules = new ArrayList<>();
        Properties properties = new Properties();

        BuildInfo buildInfo = new BuildInfoBuilder(name).started("test").version(version).number(number)
                .agent(agent).durationMillis(durationMillis).principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal).url(url).parentName(parentName).parentNumber(parentNumber)
                .modules(modules).properties(properties).buildAgent(buildAgent)
                .artifactoryPluginVersion(artifactoryPluginVersion).build();

        assertEquals(buildInfo.getVersion(), version, "Unexpected buildInfo version.");
        assertEquals(buildInfo.getName(), name, "Unexpected buildInfo name.");
        assertEquals(buildInfo.getNumber(), number, "Unexpected buildInfo number.");
        assertEquals(buildInfo.getAgent(), agent, "Unexpected buildInfo agent.");
        assertEquals(buildInfo.getBuildAgent(), buildAgent, "Unexpected buildInfo agent.");
        assertEquals(buildInfo.getDurationMillis(), durationMillis, "Unexpected buildInfo duration millis.");
        assertEquals(buildInfo.getPrincipal(), principal, "Unexpected buildInfo principal.");
        assertEquals(buildInfo.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected buildInfo artifactory principal.");
        assertEquals(buildInfo.getArtifactoryPluginVersion(), artifactoryPluginVersion, "Unexpected buildInfo artifactory Plugin Version.");
        assertEquals(buildInfo.getUrl(), url, "Unexpected buildInfo URL.");
        assertEquals(buildInfo.getParentName(), parentName, "Unexpected buildInfo parent name.");
        assertEquals(buildInfo.getParentNumber(), parentNumber, "Unexpected buildInfo parent buildInfo number.");
        assertEquals(buildInfo.getModules(), modules, "Unexpected buildInfo modules.");
        assertTrue(buildInfo.getModules().isEmpty(), "BuildInfo modules list should not have been populated.");
        assertEquals(buildInfo.getProperties(), properties, "Unexpected buildInfo properties.");
        assertTrue(buildInfo.getProperties().isEmpty(), "BuildInfo properties list should not have been populated.");
    }

    /**
     * Validates the build start time values after using the builder setters
     */
    public void testStartedSetters() {
        String started = "192-1212-1";
        BuildInfo buildInfo = new BuildInfoBuilder("test").number("4").started(started).build();
        assertEquals(buildInfo.getStarted(), started, "Unexpected buildInfo started.");

        Date startedDate = new Date();
        buildInfo = new BuildInfoBuilder("test").number("4").startedDate(startedDate).build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(buildInfo.getStarted(), simpleDateFormat.format(startedDate), "Unexpected buildInfo started.");
    }

    /**
     * Validates the build values after using the builder add methods
     */
    public void testBuilderAddMethod() {
        Module module = new Module();
        module.setId("module-id");
        String propertyKey = "key";
        String propertyValue = "value";
        PromotionStatus promotionStatus = new PromotionStatusBuilder("momo").timestampDate(new Date()).build();

        BuildInfo buildInfo = new BuildInfoBuilder("test").number("4").started("test").addModule(module)
                .addProperty(propertyKey, propertyValue).addStatus(promotionStatus).build();
        List<Module> modules = buildInfo.getModules();
        assertFalse(modules.isEmpty(), "A buildInfo module should have been added.");
        assertEquals(modules.get(0), module, "Unexpected buildInfo module.");

        assertTrue(buildInfo.getProperties().containsKey(propertyKey), "A buildInfo property should have been added.");
        assertEquals(buildInfo.getProperties().get(propertyKey), propertyValue, "Unexpected buildInfo property value.");

        List<PromotionStatus> statuses = buildInfo.getStatuses();
        assertFalse(statuses.isEmpty(), "Expected a status to be added.");
        assertEquals(statuses.get(0), promotionStatus, "Unexpected added status.");
    }

    /**
     * Validates adding duplicate modules, the builder should container only unique ones
     */
    public void testDuplicateModules() {
        Module module1 = new ModuleBuilder().type(ModuleType.MAVEN).id("id").build();
        Module module2 = new ModuleBuilder().type(ModuleType.MAVEN).id("id").build();

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1);
        builder.addModule(module2);
        BuildInfo buildInfo = builder.build();

        List<Module> modules = buildInfo.getModules();
        assertFalse(modules.isEmpty(), "A buildInfo module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");
        assertEquals(modules.get(0).getType(), "maven", "Expected to find module with type = 'maven'.");
    }

    /**
     * Validates adding same artifacts to different modules
     */
    public void testDuplicateModuleArtifacts() {
        ModuleBuilder module1 = new ModuleBuilder().type(ModuleType.MAVEN).id("id");
        module1.addArtifact(new ArtifactBuilder("artifact1").md5(MD5).sha1(SHA1).sha256(SHA2).originalDeploymentRepo(DEPLOY_REPO).build());
        module1.addArtifact(new ArtifactBuilder("artifact2").md5(MD5).sha1(SHA1).sha256(SHA2).originalDeploymentRepo(DEPLOY_REPO).build());

        ModuleBuilder module2 = new ModuleBuilder().type(ModuleType.MAVEN).id("id");
        module2.addArtifact(new ArtifactBuilder("artifact1").md5(MD5).sha1(SHA1).sha256(SHA2).originalDeploymentRepo(DEPLOY_REPO).build());
        module2.addArtifact(new ArtifactBuilder("artifact2").md5(MD5).sha1(SHA1).sha256(SHA2).originalDeploymentRepo(DEPLOY_REPO).build());

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1.build());
        builder.addModule(module2.build());
        BuildInfo buildInfo = builder.build();

        List<Module> modules = buildInfo.getModules();
        assertFalse(modules.isEmpty(), "A buildInfo module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");
        assertEquals(modules.get(0).getType(), "maven", "Expected to find module with type = 'maven'.");

        List<Artifact> artifacts = modules.get(0).getArtifacts();
        assertEquals(artifacts.size(), 2, "Expected to find only 2 artifacts.");
        assertEquals(artifacts.get(0).getName(), "artifact1", "Unexpected artifact name.");
        assertEquals(artifacts.get(0).getMd5(), MD5, "Unexpected MD5 checksum.");
        assertEquals(artifacts.get(0).getSha1(), SHA1, "Unexpected SHA-1 checksum.");
        assertEquals(artifacts.get(0).getSha256(), SHA2, "Unexpected SHA-256 checksum.");
        assertEquals(artifacts.get(1).getName(), "artifact2", "Unexpected artifact name.");
        assertEquals(artifacts.get(1).getMd5(), MD5, "Unexpected MD5 checksum.");
        assertEquals(artifacts.get(1).getSha1(), SHA1, "Unexpected SHA-1 checksum.");
        assertEquals(artifacts.get(1).getSha256(), SHA2, "Unexpected SHA-256 checksum.");
    }

    /**
     * Validates adding same dependencies with different scopes to different modules
     */
    public void testDuplicateModuleDependencies() {
        ModuleBuilder module1 = new ModuleBuilder().type(ModuleType.MAVEN).id("id");
        module1.addDependency(new DependencyBuilder().id("dep1").scopes(CommonUtils.newHashSet("compile")).build());
        module1.addDependency(new DependencyBuilder().id("dep2").scopes(CommonUtils.newHashSet("compile")).build());

        ModuleBuilder module2 = new ModuleBuilder().type(ModuleType.MAVEN).id("id");
        module2.addDependency(new DependencyBuilder().id("dep1").scopes(CommonUtils.newHashSet("compile", "test")).build());
        module2.addDependency(new DependencyBuilder().id("dep2").scopes(CommonUtils.newHashSet("compile", "test")).build());

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1.build());
        builder.addModule(module2.build());
        BuildInfo buildInfo = builder.build();

        List<Module> modules = buildInfo.getModules();
        assertFalse(modules.isEmpty(), "A buildInfo module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");
        assertEquals(modules.get(0).getType(), "maven", "Expected to find module with type = 'maven'.");

        List<Dependency> dependencies = modules.get(0).getDependencies();
        assertEquals(dependencies.size(), 2, "Expected to find only 2 dependencies.");
        assertTrue(dependencies.get(0).getScopes().contains("compile"), "Expected to find compile scope");
        assertTrue(dependencies.get(0).getScopes().contains("test"), "Expected to find test scope");
        assertTrue(dependencies.get(1).getScopes().contains("compile"), "Expected to find compile scope");
        assertTrue(dependencies.get(1).getScopes().contains("test"), "Expected to find test scope");
    }
}
