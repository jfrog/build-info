package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import static org.sonatype.aether.repository.Proxy.TYPE_HTTP;
import static org.sonatype.aether.repository.Proxy.TYPE_HTTPS;

public abstract class ArtifactoryResolutionRepositoryBase {
    protected final String releaseRepoUrl;
    protected final String snapshotRepoUrl;
    protected final String repoUsername;
    protected final String repoPassword;
    protected final Logger logger;

    public ArtifactoryResolutionRepositoryBase(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, Logger logger) {
        this.releaseRepoUrl = repoReleaseUrl;
        this.snapshotRepoUrl = snapshotRepoUrl;
        this.repoUsername = repoUsername;
        this.repoPassword = repoPassword;
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
        org.jfrog.build.extractor.Proxy p = org.jfrog.build.extractor.Proxy.createFromSystemProperties(repoUrl);
        if (p == null) {
            return null;
        }
        org.apache.maven.repository.Proxy proxy = new org.apache.maven.repository.Proxy();
        proxy.setHost(p.getHost());
        proxy.setPort(p.getPort());
        proxy.setUserName(p.getUsername());
        proxy.setPassword(p.getPassword());
        proxy.setProtocol(p.isHttps() ? "HTTPS" : "HTTP");
        return proxy;
    }

    public org.sonatype.aether.repository.Proxy createSonatypeProxy(String repoUrl) {
        org.jfrog.build.extractor.Proxy p = org.jfrog.build.extractor.Proxy.createFromSystemProperties(repoUrl);
        if (p == null) {
            return null;
        }
        return new org.sonatype.aether.repository.Proxy(
                p.isHttps() ? TYPE_HTTPS : TYPE_HTTP,
                p.getHost(),
                p.getPort(),
                StringUtils.isNotBlank(p.getUsername()) ? new org.sonatype.aether.repository.Authentication(p.getUsername(), p.getPassword()) : null);
    }

    public org.eclipse.aether.repository.Proxy createEclipProxy(String repoUrl) {
        org.jfrog.build.extractor.Proxy p = org.jfrog.build.extractor.Proxy.createFromSystemProperties(repoUrl);
        if (p == null) {
            return null;
        }
        Authentication auth = null;
        if (StringUtils.isNotBlank(p.getUsername())) {
            auth = new AuthenticationBuilder().addString("username", p.getUsername()).addSecret("password", p.getPassword()).build();
        }
        return new org.eclipse.aether.repository.Proxy(p.isHttps() ? TYPE_HTTPS : TYPE_HTTP, p.getHost(), p.getPort(), auth);
    }

    protected Boolean snapshotPolicyEnabled() {
        return StringUtils.isBlank(snapshotRepoUrl);
    }
}
