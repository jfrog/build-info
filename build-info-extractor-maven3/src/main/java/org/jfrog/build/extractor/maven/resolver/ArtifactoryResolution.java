package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jfrog.build.extractor.ProxySelector;

/**
 * Create and configure snapshot and release repositories based on the Artifactory client configuration (e.g. build info properties file)
 * Those repositories will be used instead of the default maven repositories.
 */
public class ArtifactoryResolution extends ArtifactoryResolutionRepositoryBase {
    private boolean isSnapshotEnabled;
    private String snapshotUpdatePolicy;

    public ArtifactoryResolution(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, ProxySelector proxySelector, Logger logger) {
        super(repoReleaseUrl, snapshotRepoUrl, repoUsername, repoPassword, proxySelector, logger);
        this.isSnapshotEnabled = true;
        this.snapshotUpdatePolicy = RepositoryPolicy.UPDATE_POLICY_DAILY;
    }

    public RemoteRepository createSnapshotRepository() {
        if (super.shouldCreateSnapshotRepository()) {
            return createRepository(snapshotRepoUrl, "artifactory-snapshot", false, this.isSnapshotEnabled, this.snapshotUpdatePolicy);
        }
        return null;
    }

    public RemoteRepository createReleaseRepository() {
        if (shouldCreateReleaseRepository()) {
            String repositoryId = snapshotPolicyEnabled() ? "artifactory-release-snapshot" : "artifactory-release";
            return createRepository(releaseRepoUrl, repositoryId, true, snapshotPolicyEnabled(), this.snapshotUpdatePolicy);
        }
        return null;
    }

    public ArtifactoryResolution setSnapshotEnabled(boolean isSnapshotEnabled) {
        this.isSnapshotEnabled = isSnapshotEnabled;
        return this;
    }

    public ArtifactoryResolution setSnapshotUpdatePolicy(String snapshotUpdatePolicy) {
        this.snapshotUpdatePolicy = snapshotUpdatePolicy;
        return this;
    }

    private RemoteRepository createRepository(String repoUrl, String repoId, boolean releasePolicy, Boolean snapshotPolicy, String snapshotUpdatePolicy) {
        RemoteRepository.Builder builder = new RemoteRepository.Builder(repoId, "default", repoUrl);
        setAuthentication(builder);
        setProxy(builder, repoUrl);
        setPolicy(builder, releasePolicy, snapshotPolicy, snapshotUpdatePolicy);
        return builder.build();
    }

    private void setAuthentication(RemoteRepository.Builder builder) {
        if (shouldSetAuthentication()) {
            Authentication authentication = new AuthenticationBuilder().addString("username", repoUsername)
                    .addSecret("password", repoPassword).build();
            builder.setAuthentication(authentication);
        }
    }

    private void setPolicy(RemoteRepository.Builder builder, boolean releasePolicyEnabled, boolean snapshotPolicyEnabled, String snapshotUpdatePolicy) {
        RepositoryPolicy releasePolicy = new RepositoryPolicy(releasePolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        builder.setReleasePolicy(releasePolicy);
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy(snapshotPolicyEnabled, snapshotUpdatePolicy, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        builder.setSnapshotPolicy(snapshotPolicy);
    }

    private void setProxy(RemoteRepository.Builder builder, String repoUrl) {
        Proxy proxy = createEclipseProxy(repoUrl);
        if (proxy != null) {
            builder.setProxy(proxy);
        }
    }
}
