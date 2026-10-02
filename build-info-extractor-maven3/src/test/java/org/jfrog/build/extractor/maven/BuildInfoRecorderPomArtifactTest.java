package org.jfrog.build.extractor.maven;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.builder.ModuleBuilder;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.testng.annotations.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Verify that the pom always are deployed as {@code <artifactId>-<version>.pom} regardless of if the main artifact has a
 * classifier or not.
 **/
public class BuildInfoRecorderPomArtifactTest {

    private static final String GROUP_ID = "com.example";
    private static final String ARTIFACT_ID = "feature";
    private static final String VERSION = "1.2.3";
    private static final String TARGET_REPO = "proj-example-local";
    private static final String GAV_ID = GROUP_ID + ":" + ARTIFACT_ID + ":" + VERSION;
    private static final String EXPECTED_POM_NAME = ARTIFACT_ID + "-" + VERSION + ".pom";
    private static final String EXPECTED_POM_PATH = "com/example/" + ARTIFACT_ID + "/" + VERSION + "/" + EXPECTED_POM_NAME;

    @Test
    public void classifierMainArtifactTest() throws Exception {
        assertPomRecordedAndDeployedAtExpectedPath("xml", "features");
    }

    @Test
    public void nonClassifierMainArtifactTest() throws Exception {
        assertPomRecordedAndDeployedAtExpectedPath("jar", null);
    }

    private void assertPomRecordedAndDeployedAtExpectedPath(String type, String classifier) throws Exception {
        BuildInfoRecorder recorder = new BuildInfoRecorder();
        ArtifactoryClientConfiguration config = new ArtifactoryClientConfiguration(new NullLog());
        config.publisher.setRepoKey(TARGET_REPO);
        recorder.setConfiguration(config);
        setField(recorder, "logger", new ConsoleLogger(ConsoleLogger.LEVEL_DISABLED, "test"));

        Map<String, DeployDetails> deployableArtifacts = new HashMap<>();
        setField(recorder, "deployableArtifactBuilderMap", deployableArtifacts);

        ModuleBuilder module = new ModuleBuilder().id(GAV_ID);
        currentModule(recorder).set(module);

        MavenProject project = createMavenProject(type, classifier);
        invokeAddPomArtifactIfExist(recorder, project, module);

        Module builtModule = module.build();
        assertEquals(builtModule.getArtifacts().size(), 1);
        assertEquals(builtModule.getArtifacts().get(0).getName(), EXPECTED_POM_NAME);

        assertEquals(deployableArtifacts.size(), 1);
        assertEquals(deployableArtifacts.values().iterator().next().getArtifactPath(), EXPECTED_POM_PATH);
    }

    private MavenProject createMavenProject(String type, String classifier) throws Exception {
        Model model = new Model();
        model.setGroupId(GROUP_ID);
        model.setArtifactId(ARTIFACT_ID);
        model.setVersion(VERSION);
        model.setPackaging(type);

        MavenProject project = new MavenProject(model);
        project.setArtifact(new DefaultArtifact(GROUP_ID, ARTIFACT_ID, VersionRange.createFromVersion(VERSION),
                null, type, classifier, new DefaultArtifactHandler(type)));

        File pomFile = Files.createTempFile(ARTIFACT_ID, ".pom").toFile();
        pomFile.deleteOnExit();
        project.setFile(pomFile);
        return project;
    }

    private void invokeAddPomArtifactIfExist(BuildInfoRecorder recorder, MavenProject project, ModuleBuilder module) throws Exception {
        Method method = BuildInfoRecorder.class.getDeclaredMethod(
                "addPomArtifactIfExist",
                MavenProject.class,
                ModuleBuilder.class,
                IncludeExcludePatterns.class,
                boolean.class);
        method.setAccessible(true);
        method.invoke(recorder, project, module, new IncludeExcludePatterns("", ""), false);
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<ModuleBuilder> currentModule(BuildInfoRecorder recorder) throws Exception {
        Field field = BuildInfoRecorder.class.getDeclaredField("currentModule");
        field.setAccessible(true);
        return (ThreadLocal<ModuleBuilder>) field.get(recorder);
    }

    private void setField(BuildInfoRecorder recorder, String name, Object value) throws  Exception {
        Field field = BuildInfoRecorder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(recorder, value);
    }
}
