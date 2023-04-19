package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.logging.Logger;

/**
 * Based on the Artifactory client configuration, create and configure snapshot and release repositories (e.g. build info properties file).
 * Those repositories will replace the default Maven repositories.
 */
public class ArtifactoryPluginResolution extends ArtifactoryResolutionRepositoryBase {

    public ArtifactoryPluginResolution(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, String proxyUrl, String proxyUsername, String proxyPassword, int proxyPort, boolean proxyHttps, String noProxyDomain, Logger logger) {
        super(repoReleaseUrl, snapshotRepoUrl, repoUsername, repoPassword, proxyUrl, proxyUsername, proxyPassword, proxyPort, proxyHttps, noProxyDomain, logger);
    }

    public ArtifactRepository createSnapshotRepository() {
        if (super.shouldCreateSnapshotRepository()) {
            return createDefaultRepository(snapshotRepoUrl, "artifactory-snapshot", false, true);
        }
        return null;
    }

    public ArtifactRepository createReleaseRepository() {
        if (super.shouldCreateReleaseRepository()) {
            String repositoryId = snapshotPolicyEnabled() ? "artifactory-release-snapshot" : "artifactory-release";
            return createDefaultRepository(releaseRepoUrl, repositoryId, true, snapshotPolicyEnabled());
        }
        return null;
    }

    private ArtifactRepository createDefaultRepository(String repoUrl, String repoId, Boolean releasePolicy, Boolean snapshotPolicy) {
        ArtifactRepository repository = new MavenArtifactRepository();
        setPolicy(repository, releasePolicy, snapshotPolicy);
        repository.setLayout(new DefaultRepositoryLayout());
        repository.setUrl(repoUrl);
        repository.setId(repoId);
        setAuth(repository);
        setProxy(repository);

        return repository;
    }

    private void setPolicy(ArtifactRepository snapshotPluginRepository, boolean releasePolicyEnabled, boolean snapshotPolicyEnabled) {
        ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(releasePolicyEnabled, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
        snapshotPluginRepository.setReleaseUpdatePolicy(releasePolicy);
        ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(snapshotPolicyEnabled, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
        snapshotPluginRepository.setSnapshotUpdatePolicy(snapshotPolicy);
    }

    private void setAuth(ArtifactRepository snapshotPluginRepository) {
        if (shouldSetAuthentication()) {
            org.apache.maven.artifact.repository.Authentication authentication = new org.apache.maven.artifact.repository.Authentication(repoUsername, repoPassword);
            snapshotPluginRepository.setAuthentication(authentication);
        }
    }

    private void setProxy(ArtifactRepository snapshotPluginRepository) {
        if (shouldSetProxy()) {
            org.apache.maven.repository.Proxy proxy = getProxy();
            snapshotPluginRepository.setProxy(proxy);
        }
    }

    private org.apache.maven.repository.Proxy getProxy() {
        org.apache.maven.repository.Proxy proxy = new org.apache.maven.repository.Proxy();
        proxy.setHost(proxyUrl);
        proxy.setPort(proxyPort);
        proxy.setUserName(proxyUsername);
        proxy.setPassword(proxyPassword);
        proxy.setProtocol(proxyHttps ? "HTTPS" : "HTTP");
        proxy.setNonProxyHosts(noProxyDomain);
        return proxy;
    }

}
