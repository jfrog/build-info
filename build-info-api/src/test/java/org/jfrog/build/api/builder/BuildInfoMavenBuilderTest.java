package org.jfrog.build.api.builder;

import org.jfrog.build.api.*;
import org.jfrog.build.api.release.PromotionStatus;
import org.jfrog.build.api.util.CommonUtils;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildType.GRADLE;
import static org.testng.Assert.*;

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

    /**
     * Validates the build values when using the defaults
     */
    public void testDefaultBuild() {
        Build build = new BuildInfoBuilder("test").number("4").started("test").build();
        assertEquals(build.getVersion(), "1.0.1", "Unexpected default build version.");
        assertEquals(build.getNumber(), "4", "Unexpected default build number.");
        assertNull(build.getType(), "Default build type should be null.");

        assertNull(build.getAgent(), "Default build agent should be null.");

        assertEquals(build.getDurationMillis(), 0, "Default build duration millis should be zero.");
        assertNull(build.getPrincipal(), "Default build principal should be null.");
        assertNull(build.getArtifactoryPrincipal(), "Default build artifactory principal should be null.");
        assertNull(build.getArtifactoryPluginVersion(), "Default build ArtifactoryPluginVersion should be null.");
        assertNull(build.getUrl(), "Default build URL should be null.");
        assertNull(build.getParentName(), "Default build parent build name should be null.");
        assertNull(build.getParentNumber(), "Default build parent build number should be null.");
        assertNull(build.getModules(), "Default build modules should be null.");
        assertNull(build.getProperties(), "Default properties should be null.");
        assertNull(build.getVcsRevision(), "Default vcs revision should be null.");
    }

    /**
     * Validates the build values after using the builder setters
     */
    public void testBuilderSetters() {
        String version = "1.2.0";
        String name = "moo";
        String number = "15";
        BuildType buildType = GRADLE;
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

        Build build = new BuildInfoBuilder(name).started("test").version(version).number(number).type(buildType)
                .agent(agent).durationMillis(durationMillis).principal(principal)
                .artifactoryPrincipal(artifactoryPrincipal).url(url).parentName(parentName).parentNumber(parentNumber)
                .modules(modules).properties(properties).buildAgent(buildAgent)
                .artifactoryPluginVersion(artifactoryPluginVersion).build();

        assertEquals(build.getVersion(), version, "Unexpected build version.");
        assertEquals(build.getName(), name, "Unexpected build name.");
        assertEquals(build.getNumber(), number, "Unexpected build number.");
        assertEquals(build.getType(), buildType, "Unexpected build type.");
        assertEquals(build.getAgent(), agent, "Unexpected build agent.");
        assertEquals(build.getBuildAgent(), buildAgent, "Unexpected build agent.");
        assertEquals(build.getDurationMillis(), durationMillis, "Unexpected build duration millis.");
        assertEquals(build.getPrincipal(), principal, "Unexpected build principal.");
        assertEquals(build.getArtifactoryPrincipal(), artifactoryPrincipal, "Unexpected build artifactory principal.");
        assertEquals(build.getArtifactoryPluginVersion(), artifactoryPluginVersion, "Unexpected build artifactory Plugin Version.");
        assertEquals(build.getUrl(), url, "Unexpected build URL.");
        assertEquals(build.getParentName(), parentName, "Unexpected build parent name.");
        assertEquals(build.getParentNumber(), parentNumber, "Unexpected build parent build number.");
        assertEquals(build.getModules(), modules, "Unexpected build modules.");
        assertTrue(build.getModules().isEmpty(), "Build modules list should not have been populated.");
        assertEquals(build.getProperties(), properties, "Unexpected build properties.");
        assertTrue(build.getProperties().isEmpty(), "Build properties list should not have been populated.");
    }

    /**
     * Validates the build start time values after using the builder setters
     */
    public void testStartedSetters() throws ParseException {
        String started = "192-1212-1";
        Build build = new BuildInfoBuilder("test").number("4").started(started).build();
        assertEquals(build.getStarted(), started, "Unexpected build started.");

        Date startedDate = new Date();
        build = new BuildInfoBuilder("test").number("4").startedDate(startedDate).build();

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        assertEquals(build.getStarted(), simpleDateFormat.format(startedDate), "Unexpected build started.");
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

        Build build = new BuildInfoBuilder("test").number("4").started("test").addModule(module)
                .addProperty(propertyKey, propertyValue).addStatus(promotionStatus).build();
        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.get(0), module, "Unexpected build module.");

        assertTrue(build.getProperties().containsKey(propertyKey), "A build property should have been added.");
        assertEquals(build.getProperties().get(propertyKey), propertyValue, "Unexpected build property value.");

        List<PromotionStatus> statuses = build.getStatuses();
        assertFalse(statuses.isEmpty(), "Expected a status to be added.");
        assertEquals(statuses.get(0), promotionStatus, "Unexpected added status.");
    }

    /**
     * Validates adding duplicate modules, the builder should container only unique ones
     */
    public void testDuplicateModules() {
        Module module1 = new ModuleBuilder().id("id").build();
        Module module2 = new ModuleBuilder().id("id").build();

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1);
        builder.addModule(module2);
        Build build = builder.build();

        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");
    }

    /**
     * Validates adding same artifacts to different modules
     */
    public void testDuplicateModuleArtifacts() {
        ModuleBuilder module1 = new ModuleBuilder().id("id");
        module1.addArtifact(new ArtifactBuilder("artifact1").md5(MD5).sha1(SHA1).sha256(SHA2).build());
        module1.addArtifact(new ArtifactBuilder("artifact2").md5(MD5).sha1(SHA1).sha256(SHA2).build());

        ModuleBuilder module2 = new ModuleBuilder().id("id");
        module2.addArtifact(new ArtifactBuilder("artifact1").md5(MD5).sha1(SHA1).sha256(SHA2).build());
        module2.addArtifact(new ArtifactBuilder("artifact2").md5(MD5).sha1(SHA1).sha256(SHA2).build());

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1.build());
        builder.addModule(module2.build());
        Build build = builder.build();

        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");

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
        ModuleBuilder module1 = new ModuleBuilder().id("id");
        module1.addDependency(new DependencyBuilder().id("dep1").scopes(CommonUtils.newHashSet("compile")).build());
        module1.addDependency(new DependencyBuilder().id("dep2").scopes(CommonUtils.newHashSet("compile")).build());

        ModuleBuilder module2 = new ModuleBuilder().id("id");
        module2.addDependency(new DependencyBuilder().id("dep1").scopes(CommonUtils.newHashSet("compile", "test")).build());
        module2.addDependency(new DependencyBuilder().id("dep2").scopes(CommonUtils.newHashSet("compile", "test")).build());

        BuildInfoMavenBuilder builder = new BuildInfoMavenBuilder("test").number("4").started("test");
        builder.addModule(module1.build());
        builder.addModule(module2.build());
        Build build = builder.build();

        List<Module> modules = build.getModules();
        assertFalse(modules.isEmpty(), "A build module should have been added.");
        assertEquals(modules.size(), 1, "Expected to find only 1 module.");
        assertEquals(modules.get(0).getId(), "id", "Expected to find module with id = 'id'.");

        List<Dependency> dependencies = modules.get(0).getDependencies();
        assertEquals(dependencies.size(), 2, "Expected to find only 2 dependencies.");
        assertTrue(dependencies.get(0).getScopes().contains("compile"), "Expected to find compile scope");
        assertTrue(dependencies.get(0).getScopes().contains("test"), "Expected to find test scope");
        assertTrue(dependencies.get(1).getScopes().contains("compile"), "Expected to find compile scope");
        assertTrue(dependencies.get(1).getScopes().contains("test"), "Expected to find test scope");
    }
}
