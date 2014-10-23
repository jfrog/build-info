package org.jfrog.build.extractor.maven.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.internal.impl.DefaultArtifactResolver;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import javax.inject.Named;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Named
@Component( role = ArtifactoryEclipseArtifactResolver.class )
public class ArtifactoryEclipseArtifactResolver extends DefaultArtifactResolver {

    @Requirement
    private Logger logger;

    @Requirement
    private ResolutionHelper resolutionHelper;

    private List<RemoteRepository> resolutionRepositories = null;

    /**
     * Create a list containing one release and one snapshot resolution repositories, according to the configuration in the Artifactory plugin.
     * The list is used to override Maven's default or configured repositories, so that the build dependencies are resolved from Artifactory.
     * The list is saved and reused for further invokations to this method.
     * @param session
     * @return
     * @throws Exception
     */
    private List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) throws Exception {
        if (resolutionRepositories == null) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(session.getSystemProperties());
            allMavenProps.putAll(session.getUserProperties());
            resolutionHelper.init(allMavenProps);

            RemoteRepository releaseRepository = null;
            RemoteRepository snapshotRepository = null;
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
            }

            List<RemoteRepository> tempRepositories = Lists.newArrayList();
            if (releaseRepository != null) {
                tempRepositories.add(releaseRepository);
            }
            if (snapshotRepository != null) {
                tempRepositories.add(snapshotRepository);
            }
            resolutionRepositories = tempRepositories;
        }
        return resolutionRepositories;
    }

    private void enforceResolutionRepositories(RepositorySystemSession session, ArtifactRequest request) throws ArtifactResolutionException {
        List<RemoteRepository> repositories;
        try {
            // Get the Artifactory repositories configured in the Artifactory plugin:
            repositories = getResolutionRepositories(session);
        } catch (Exception e) {
            List<ArtifactResult> emptyList = Collections.emptyList();
            throw new ArtifactResolutionException(emptyList, "Failed while creating Artifactory resolution repositories: " + e.getMessage(), e);
        }
        // The repositories list can be empty, in case this build is not running from a CI server.
        // In that case, we do not want to override Maven's configured repositories:
        if (repositories != null && !repositories.isEmpty()) {
            request.setRepositories(repositories);
        }
    }

    /**
     * Before letting Maven resolve the artifact, this method enforces the configured Artifactory resolution repositories.
     */
    @Override
    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session, Collection<? extends ArtifactRequest> requests )
        throws ArtifactResolutionException {

        for(ArtifactRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the artifacts:
        return super.resolveArtifacts(session, requests);
    }
}
