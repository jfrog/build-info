package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.logging.Logger;

public abstract class ArtifactoryResolutionRepositoryBase {
    protected final String releaseRepoUrl;
    protected final String snapshotRepoUrl;
    protected final String repoUsername;
    protected final String repoPassword;
    protected final String proxyUrl;
    protected final String proxyUsername;
    protected final String proxyPassword;
    protected final int proxyPort;
    protected final boolean proxyHttps;
    protected final String noProxyDomain;
    protected final Logger logger;

    public ArtifactoryResolutionRepositoryBase(String repoReleaseUrl, String snapshotRepoUrl, String repoUsername, String repoPassword, String proxyUrl, String proxyUsername, String proxyPassword, int proxyPort, boolean proxyHttps, String NoProxyDomain, Logger logger) {
        this.releaseRepoUrl = repoReleaseUrl;
        this.snapshotRepoUrl = snapshotRepoUrl;
        this.repoUsername = repoUsername;
        this.repoPassword = repoPassword;
        this.proxyUrl = proxyUrl;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
        this.proxyPort = proxyPort;
        this.proxyHttps = proxyHttps;
        this.noProxyDomain = NoProxyDomain;
        this.logger = logger;
    }

    protected boolean shouldCreateSnapshotRepository() {
        if (StringUtils.isBlank(snapshotRepoUrl)) {
            return false;
        }
        logger.debug("[buildinfo] Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
        return true;
    }

    protected boolean shouldCreateReleaseRepository() {
        if (StringUtils.isBlank(releaseRepoUrl)) {
            return false;
        }
        logger.debug("[buildinfo] Enforcing release repository for resolution: " + releaseRepoUrl);
        return true;
    }

    protected boolean shouldSetAuthentication() {
        if (StringUtils.isBlank(repoUsername)) {
            return false;
        }
        logger.debug("[buildinfo] Enforcing repository for snapshot resolution repository");
        return true;
    }

    protected boolean shouldSetProxy() {
        if (StringUtils.isBlank(proxyUrl)) {
            return false;
        }
        logger.debug("[buildinfo] Enforcing proxy: " + proxyUrl + " for snapshot resolution repository");
        return true;
    }

    protected Boolean snapshotPolicyEnabled() {
        return StringUtils.isBlank(snapshotRepoUrl);
    }
}
