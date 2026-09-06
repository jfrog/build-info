package org.jfrog.build.extractor.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.Snapshot;
import org.apache.maven.artifact.repository.metadata.Versioning;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builds the unique snapshot versions - "1.0-20240101.101112-3" - that Maven 3 puts in deployed snapshot file
 * names. The recorder takes deployment away from maven-deploy-plugin, which means the version transformation
 * the plugin would normally apply never runs, so the extractor has to reproduce it here.
 *
 * @see BuildInfoRecorder
 */
@Component(role = SnapshotVersionResolver.class)
public class SnapshotVersionResolver {

    public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss").withZone(ZoneOffset.UTC);

    @Requirement
    private Logger logger;

    @Requirement
    private ArtifactoryManagerBuilder artifactoryManagerBuilder;

    /**
     * groupId:artifactId:baseVersion to unique version, so that every artifact of a module - the jar, the pom
     * and any attached artifact - is deployed under one version.
     */
    private final Map<String, String> uniqueVersions = new ConcurrentHashMap<>();
    private ArtifactoryClientConfiguration conf;
    /**
     * The whole session shares one timestamp, as maven-deploy-plugin does. Null turns unique snapshots off.
     */
    private String timestamp;
    /**
     * Turned off after the first time Artifactory could not be reached, so that an unreachable or misconfigured
     * publisher costs one failed request per build rather than one per module.
     */
    private boolean metadataReadable;

    public void newSession(ArtifactoryClientConfiguration conf, Date sessionStart, boolean uniqueSnapshots) {
        this.conf = conf;
        this.timestamp = uniqueSnapshots
                ? TIMESTAMP_FORMAT.format((sessionStart == null ? new Date() : sessionStart).toInstant())
                : null;
        this.metadataReadable = uniqueSnapshots;
        uniqueVersions.clear();
    }

    public static boolean isSnapshot(String version) {
        return StringUtils.endsWith(version, SNAPSHOT_SUFFIX);
    }

    /**
     * @return the version to write into the deployed file name. Snapshots get a timestamp and a build number
     * appended in place of the -SNAPSHOT suffix; every other version is returned untouched.
     */
    public String getDeployedVersion(String groupId, String artifactId, String version) {
        if (timestamp == null || !isSnapshot(version)) {
            return version;
        }
        String key = groupId + ":" + artifactId + ":" + version;
        String cached = uniqueVersions.get(key);
        if (cached != null) {
            return cached;
        }
        String uniqueVersion = StringUtils.removeEnd(version, SNAPSHOT_SUFFIX) + "-" + timestamp + "-"
                + resolveBuildNumber(groupId, artifactId, version);
        String published = uniqueVersions.putIfAbsent(key, uniqueVersion);
        return published == null ? uniqueVersion : published;
    }

    /**
     * Maven derives the build number from the snapshot metadata already sitting in the repository. Metadata we
     * cannot read means we cannot tell how many builds came before, and deploying as the first one keeps the
     * timestamp - and with it the traceability the unique version exists for - intact.
     */
    private int resolveBuildNumber(String groupId, String artifactId, String baseVersion) {
        if (!metadataReadable) {
            return 1;
        }
        String repoKey = StringUtils.defaultIfBlank(conf.publisher.getSnapshotRepoKey(), conf.publisher.getRepoKey());
        if (StringUtils.isBlank(repoKey)) {
            return 1;
        }
        String metadataPath = repoKey + "/" + groupId.replace('.', '/') + "/" + artifactId + "/" + baseVersion
                + "/maven-metadata.xml";
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.resolveProperties(conf)) {
            String metadata = artifactoryManager.download(metadataPath).getContent();
            return readBuildNumber(metadata) + 1;
        } catch (FileNotFoundException e) {
            // Nothing deployed under this version yet, which is the normal case for a first snapshot deployment.
            logger.debug("Artifactory Build Info Recorder: no snapshot metadata at '" + metadataPath + "'.");
        } catch (Exception e) {
            metadataReadable = false;
            logger.warn("Artifactory Build Info Recorder: failed reading snapshot metadata from '" + metadataPath
                    + "', snapshots of this build will be deployed with build number 1: " + e.getMessage());
        }
        return 1;
    }

    private int readBuildNumber(String metadataXml) throws Exception {
        Metadata metadata = new MetadataXpp3Reader().read(new StringReader(metadataXml), false);
        Versioning versioning = metadata.getVersioning();
        Snapshot snapshot = versioning == null ? null : versioning.getSnapshot();
        return snapshot == null ? 0 : snapshot.getBuildNumber();
    }
}
