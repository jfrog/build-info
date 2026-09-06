package org.jfrog.build.extractor.maven;

import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.easymock.EasyMock;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.client.DownloadResponse;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.util.Date;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author naveenku
 */
public class SnapshotVersionResolverTest {

    private static final String GROUP_ID = "org.example";
    private static final String ARTIFACT_ID = "my-app";
    private static final String SNAPSHOT_VERSION = "1.0-SNAPSHOT";
    private static final String SNAPSHOT_REPO = "libs-snapshot-local";
    // new Date(0) is 1970-01-01T00:00:00Z, so the session timestamp is deterministic.
    private static final Date EPOCH = new Date(0);
    private static final String EPOCH_TIMESTAMP = "19700101.000000";
    private static final String EXPECTED_METADATA_PATH =
            SNAPSHOT_REPO + "/org/example/my-app/1.0-SNAPSHOT/maven-metadata.xml";

    private SnapshotVersionResolver resolver;
    private ArtifactoryManagerBuilder managerBuilder;
    private ArtifactoryManager artifactoryManager;
    private ArtifactoryClientConfiguration conf;

    @BeforeMethod
    public void setUp() throws Exception {
        resolver = new SnapshotVersionResolver();
        managerBuilder = EasyMock.createMock(ArtifactoryManagerBuilder.class);
        artifactoryManager = EasyMock.createMock(ArtifactoryManager.class);
        inject(resolver, "logger", new ConsoleLogger(ConsoleLogger.LEVEL_DISABLED, "test"));
        inject(resolver, "artifactoryManagerBuilder", managerBuilder);

        conf = new ArtifactoryClientConfiguration(new NullLog());
        conf.publisher.setSnapshotRepoKey(SNAPSHOT_REPO);
    }

    @Test
    public void isSnapshotTest() {
        assertTrue(SnapshotVersionResolver.isSnapshot("1.0-SNAPSHOT"));
        assertFalse(SnapshotVersionResolver.isSnapshot("1.0"));
        assertFalse(SnapshotVersionResolver.isSnapshot(null));
    }

    /**
     * Releases are never rewritten, and no metadata lookup is made for them.
     */
    @Test
    public void releaseVersionUnchangedTest() {
        EasyMock.replay(managerBuilder, artifactoryManager);
        resolver.newSession(conf, EPOCH, true);
        assertEquals(resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, "1.0"), "1.0");
        EasyMock.verify(managerBuilder, artifactoryManager);
    }

    /**
     * -DuniqueVersion=false is honored: the snapshot keeps its -SNAPSHOT name and nothing is looked up.
     */
    @Test
    public void uniqueSnapshotsDisabledTest() {
        EasyMock.replay(managerBuilder, artifactoryManager);
        resolver.newSession(conf, EPOCH, false);
        assertEquals(resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION), SNAPSHOT_VERSION);
        EasyMock.verify(managerBuilder, artifactoryManager);
    }

    /**
     * A first snapshot deployment - no metadata yet - is deployed as build number 1.
     */
    @Test
    public void firstSnapshotDeploymentTest() throws Exception {
        expectMetadataDownload();
        EasyMock.expect(artifactoryManager.download(EXPECTED_METADATA_PATH)).andThrow(new FileNotFoundException());
        EasyMock.replay(managerBuilder, artifactoryManager);

        resolver.newSession(conf, EPOCH, true);
        assertEquals(resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION),
                "1.0-" + EPOCH_TIMESTAMP + "-1");
        EasyMock.verify(managerBuilder, artifactoryManager);
    }

    /**
     * The build number is derived from the snapshot metadata already in the repository.
     */
    @Test
    public void buildNumberFromMetadataTest() throws Exception {
        expectMetadataDownload();
        EasyMock.expect(artifactoryManager.download(EXPECTED_METADATA_PATH))
                .andReturn(new DownloadResponse(snapshotMetadata(4), null));
        EasyMock.replay(managerBuilder, artifactoryManager);

        resolver.newSession(conf, EPOCH, true);
        assertEquals(resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION),
                "1.0-" + EPOCH_TIMESTAMP + "-5");
        EasyMock.verify(managerBuilder, artifactoryManager);
    }

    /**
     * Every artifact of a module - jar, pom, sources - must share one unique version, so the metadata is read
     * only once per group:artifact:version and the result is reused.
     */
    @Test
    public void sameVersionForRepeatedGavTest() throws Exception {
        expectMetadataDownload();
        EasyMock.expect(artifactoryManager.download(EXPECTED_METADATA_PATH)).andThrow(new FileNotFoundException());
        EasyMock.replay(managerBuilder, artifactoryManager);

        resolver.newSession(conf, EPOCH, true);
        String first = resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION);
        String second = resolver.getDeployedVersion(GROUP_ID, ARTIFACT_ID, SNAPSHOT_VERSION);
        assertEquals(second, first);
        // download expected exactly once - verify() fails if it was called again.
        EasyMock.verify(managerBuilder, artifactoryManager);
    }

    private void expectMetadataDownload() {
        EasyMock.expect(managerBuilder.resolveProperties(conf)).andReturn(artifactoryManager);
        artifactoryManager.close();
        EasyMock.expectLastCall();
    }

    private static String snapshotMetadata(int buildNumber) {
        return "<metadata>"
                + "<groupId>" + GROUP_ID + "</groupId>"
                + "<artifactId>" + ARTIFACT_ID + "</artifactId>"
                + "<version>" + SNAPSHOT_VERSION + "</version>"
                + "<versioning><snapshot>"
                + "<timestamp>20240101.101112</timestamp>"
                + "<buildNumber>" + buildNumber + "</buildNumber>"
                + "</snapshot><lastUpdated>20240101101112</lastUpdated></versioning>"
                + "</metadata>";
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
