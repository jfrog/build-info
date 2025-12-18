package org.jfrog.build.extractor.maven.resolver;

import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jfrog.build.extractor.Proxy;
import org.jfrog.build.extractor.ProxySelector;
import org.slf4j.Logger;

import static org.eclipse.aether.repository.Proxy.TYPE_HTTP;
import static org.eclipse.aether.repository.Proxy.TYPE_HTTPS;

public abstract class ArtifactoryResolutionRepositoryBase {
    protected final String releaseRepoUrl;
    protected final String snapshotRepoUrl;
    protected final String repoUsername;
    protected final String repoPassword;
    private final ProxySelector proxySelector;
    protected final Logger logger;

    public ArtifactoryResolutionRepositoryBase(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, ProxySelector proxySelector, Logger logger) {
        this.releaseRepoUrl = repoReleaseUrl;
        this.snapshotRepoUrl = snapshotRepoUrl;
        this.repoUsername = repoUsername;
        this.repoPassword = repoPassword;
        this.proxySelector = proxySelector;
        this.logger = logger;
    }

    protected boolean shouldCreateSnapshotRepository() {
        if (StringUtils.isBlank(snapshotRepoUrl)) {
            return false;
        }
        logger.debug("[build-info] Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
        return true;
    }

    protected boolean shouldCreateReleaseRepository() {
        if (StringUtils.isBlank(releaseRepoUrl)) {
            return false;
        }
        logger.debug("[build-info] Enforcing release repository for resolution: " + releaseRepoUrl);
        return true;
    }

    protected boolean shouldSetAuthentication() {
        if (StringUtils.isBlank(repoUsername)) {
            return false;
        }
        logger.debug("[build-info] Enforcing repository for snapshot resolution repository");
        return true;
    }

    public org.apache.maven.repository.Proxy createMavenProxy(String repoUrl) {
        Proxy proxyConfig = proxySelector.getProxy(repoUrl);
        if (proxyConfig == null) {
            return null;
        }
        org.apache.maven.repository.Proxy proxy = new org.apache.maven.repository.Proxy();
        proxy.setHost(proxyConfig.getHost());
        proxy.setPort(proxyConfig.getPort());
        proxy.setUserName(proxyConfig.getUsername());
        proxy.setPassword(proxyConfig.getPassword());
        proxy.setProtocol(proxyConfig.isHttps() ? "HTTPS" : "HTTP");
        return proxy;
    }

    public org.eclipse.aether.repository.Proxy createEclipseProxy(String repoUrl) {
        Proxy proxyConfig = proxySelector.getProxy(repoUrl);
        if (proxyConfig == null) {
            return null;
        }
        Authentication auth = null;
        if (StringUtils.isNotBlank(proxyConfig.getUsername())) {
            auth = new AuthenticationBuilder().addString("username", proxyConfig.getUsername()).addSecret("password", proxyConfig.getPassword()).build();
        }
        return new org.eclipse.aether.repository.Proxy(proxyConfig.isHttps() ? TYPE_HTTPS : TYPE_HTTP, proxyConfig.getHost(), proxyConfig.getPort(), auth);
    }

    protected boolean snapshotPolicyEnabled() {
        return StringUtils.isBlank(snapshotRepoUrl);
    }
}
