package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Named
@Component(role = ArtifactoryEclipseResolversHelper.class)
public class ArtifactoryEclipseResolversHelper {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    private List<ArtifactRepository> resolutionPluginRepositories = null;
    private List<RemoteRepository> resolutionRepositories = null;
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

    void initResolutionRepositories(RepositorySystemSession session) {
        getResolutionRepositories(session);
    }

    /**
     * Create a list containing one release and one snapshot resolution repositories, according to the configuration in the Artifactory plugin.
     * The list is used to override Maven's default or configured repositories, so that the build dependencies are resolved from Artifactory.
     * The list is saved and reused for further invocations to this method.
     *
     * @param session - Settings and components that control the repository system
     * @return Snapshot and release resolution repositories
     */
    List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
        if (resolutionRepositories == null) {
            List<RemoteRepository> tempRepositories = new ArrayList<>();
            initResolutionHelper(session);

            String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
            String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

            Authentication authentication = null;
            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                authentication = new AuthenticationBuilder().addString("username", resolutionHelper.getRepoUsername())
                        .addSecret("password", resolutionHelper.getRepoPassword()).build();
            }
            Proxy proxy = null;
            if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                Authentication auth = new AuthenticationBuilder()
                        .addString("username", resolutionHelper.getProxyUsername()).addSecret("password", resolutionHelper.getProxyPassword()).build();
                proxy = new Proxy(null, resolutionHelper.getProxyHost(), resolutionHelper.getProxyPort(), auth);
            }

            if (StringUtils.isNotBlank(snapshotRepoUrl)) {
                logger.debug("[buildinfo] Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
                RepositoryPolicy releasePolicy = new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RemoteRepository.Builder builder = new RemoteRepository.Builder("artifactory-snapshot", "default", snapshotRepoUrl);
                builder.setReleasePolicy(releasePolicy);
                builder.setSnapshotPolicy(snapshotPolicy);
                if (authentication != null) {
                    logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for snapshot resolution repository");
                    builder.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for snapshot resolution repository");
                    builder.setProxy(proxy);
                }
                snapshotRepository = builder.build();
                tempRepositories.add(snapshotRepository);
            }

            if (StringUtils.isNotBlank(releaseRepoUrl)) {
                logger.debug("[buildinfo] Enforcing release repository for resolution: " + releaseRepoUrl);
                boolean snapshotPolicyEnabled = snapshotRepository == null;
                String repositoryId = snapshotPolicyEnabled ? "artifactory-release-snapshot" : "artifactory-release";

                RepositoryPolicy releasePolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(snapshotPolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RemoteRepository.Builder builder = new RemoteRepository.Builder(repositoryId, "default", releaseRepoUrl);
                builder.setReleasePolicy(releasePolicy);
                builder.setSnapshotPolicy(snapshotPolicy);
                if (authentication != null) {
                    logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for release resolution repository");
                    builder.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for release resolution repository");
                    builder.setProxy(proxy);
                }
                releaseRepository = builder.build();
                tempRepositories.add(releaseRepository);
            }
            resolutionRepositories = tempRepositories;
        }
        return resolutionRepositories;
    }

    List<ArtifactRepository> getResolutionPluginRepositories(RepositorySystemSession session) {
        if (resolutionPluginRepositories == null) {
            List<ArtifactRepository> tempRepositories = new ArrayList<>();
            initResolutionHelper(session);

            String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
            String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

            org.apache.maven.artifact.repository.Authentication authentication = null;
            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                authentication = new org.apache.maven.artifact.repository.Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
            }
            org.apache.maven.repository.Proxy proxy = null;
            if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                proxy = new org.apache.maven.repository.Proxy();
                proxy.setHost(resolutionHelper.getProxyHost());
                proxy.setPort(resolutionHelper.getProxyPort());
                proxy.setUserName(resolutionHelper.getProxyUsername());
                proxy.setPassword(resolutionHelper.getProxyPassword());
            }

            if (StringUtils.isNotBlank(snapshotRepoUrl)) {
                logger.debug("[buildinfo] Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
                ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(false, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
                ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
                ArtifactRepository snapshotPluginRepository = new MavenArtifactRepository("artifactory-snapshot", snapshotRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);
                if (authentication != null) {
                    logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for snapshot resolution repository");
                    snapshotPluginRepository.setAuthentication(authentication);
                }

                if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                    authentication = new org.apache.maven.artifact.repository.Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
                }
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for snapshot resolution repository");
                    snapshotPluginRepository.setProxy(proxy);
                }
                tempRepositories.add(snapshotPluginRepository);
            }

            if (StringUtils.isNotBlank(releaseRepoUrl)) {
                logger.debug("[buildinfo] Enforcing release repository for resolution: " + releaseRepoUrl);
                boolean snapshotPolicyEnabled = tempRepositories.isEmpty();
                String repositoryId = snapshotPolicyEnabled ? "artifactory-release-snapshot" : "artifactory-release";

                ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
                ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(snapshotPolicyEnabled, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
                ArtifactRepository releasePluginRepository = new MavenArtifactRepository(repositoryId, releaseRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);
                if (authentication != null) {
                    logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for release resolution repository");
                    releasePluginRepository.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for release resolution repository");
                    releasePluginRepository.setProxy(proxy);
                }
                tempRepositories.add(releasePluginRepository);
            }

            resolutionPluginRepositories = tempRepositories;
        }
        return resolutionPluginRepositories;
    }

    RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        if (snapshotRepository != null) {
            return snapshotRepository;
        }
        return releaseRepository;
    }

    RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        return releaseRepository;
    }

    private void initResolutionHelper(RepositorySystemSession session) {
        if (!resolutionHelper.isInitialized()) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(session.getSystemProperties());
            allMavenProps.putAll(session.getUserProperties());
            resolutionHelper.init(allMavenProps);
        }

    }
}
