package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;
import org.sonatype.aether.util.repository.DefaultProxySelector;

import static org.sonatype.aether.repository.Proxy.TYPE_HTTP;
import static org.sonatype.aether.repository.Proxy.TYPE_HTTPS;

/**
 * Based on the Artifactory client configuration, create and configure snapshot and release repositories (e.g. build info properties file).
 * Those repositories will replace the default Maven repositories.
 */
public class ArtifactorySonatypeResolution extends ArtifactoryResolutionRepositoryBase {

    public ArtifactorySonatypeResolution(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, String proxyUrl, String proxyUsername, String proxyPassword, int proxyPort, boolean proxyHttps, String noProxyDomain, Logger logger) {
        super(repoReleaseUrl, snapshotRepoUrl, repoUsername, repoPassword, proxyUrl, proxyUsername, proxyPassword, proxyPort, proxyHttps, noProxyDomain, logger);
    }

    public RemoteRepository createSnapshotRepository() {
        if (super.shouldCreateSnapshotRepository()) {
            return createDefaultRepository(snapshotRepoUrl, "artifactory-snapshot", false, true);
        }
        return null;
    }

    public RemoteRepository createReleaseRepository() {
        if (super.shouldCreateReleaseRepository()) {
            String repositoryId = snapshotPolicyEnabled() ? "artifactory-release-snapshot" : "artifactory-release";
            return createDefaultRepository(releaseRepoUrl, repositoryId, true, snapshotPolicyEnabled());
        }
        return null;
    }

    private RemoteRepository createDefaultRepository(String repoUrl, String repoId, Boolean releasePolicy, Boolean snapshotPolicy) {
        RemoteRepository repository = new RemoteRepository();
        setPolicy(repository, releasePolicy, snapshotPolicy);
        repository.setUrl(repoUrl);
        repository.setId(repoId);
        setAuth(repository);
        setProxy(repository);

        return repository;
    }

    private void setPolicy(RemoteRepository snapshotPluginRepository, boolean releasePolicyEnabled, boolean snapshotPolicyEnabled) {
        RepositoryPolicy releasePolicy = new RepositoryPolicy(releasePolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        snapshotPluginRepository.setPolicy(snapshotPolicyEnabled, releasePolicy);
        RepositoryPolicy snapshotPolicy = new RepositoryPolicy(snapshotPolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
        snapshotPluginRepository.setPolicy(snapshotPolicyEnabled, snapshotPolicy);
    }

    private void setAuth(RemoteRepository snapshotPluginRepository) {
        if (shouldSetAuthentication()) {
            Authentication authentication = new Authentication(repoUsername, repoPassword);
            snapshotPluginRepository.setAuthentication(authentication);
        }
    }

    private void setProxy(RemoteRepository snapshotPluginRepository) {
        if (shouldSetProxy()) {
            Proxy proxy = new Proxy(proxyHttps ? TYPE_HTTPS : TYPE_HTTP, proxyUrl, proxyPort,new Authentication(proxyUsername, proxyPassword));
            DefaultProxySelector proxySelector = new DefaultProxySelector();
            proxySelector.add(proxy, noProxyDomain);
            snapshotPluginRepository.setProxy(proxySelector.getProxy(new RemoteRepository(snapshotPluginRepository)));
        }
    }

}
