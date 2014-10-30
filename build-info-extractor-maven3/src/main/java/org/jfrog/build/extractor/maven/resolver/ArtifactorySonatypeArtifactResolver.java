package org.jfrog.build.extractor.maven.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.impl.internal.DefaultArtifactResolver;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResolutionException;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.repository.*;

import javax.inject.Named;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * The class extends Maven's DefaultArtifactResolver and adds to it code that enforces Artifactory's configured resolution repositories.
 */
@Named
@Component( role = ArtifactorySonatypeArtifactResolver.class )
public class ArtifactorySonatypeArtifactResolver extends DefaultArtifactResolver {

    @Requirement
    private Logger logger;

    @Requirement
    private ResolutionHelper resolutionHelper;

    private List<RemoteRepository> resolutionRepositories = null;
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

    public void initResolutionRepositories(RepositorySystemSession session) {
        getResolutionRepositories(session);
    }

    public List<RemoteRepository> getResolutionRepositories(RepositorySystemSession session) {
        if (resolutionRepositories == null) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(session.getSystemProperties());
            allMavenProps.putAll(session.getUserProperties());
            resolutionHelper.init(allMavenProps);

            String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
            String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

            Authentication authentication = null;
            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                authentication = new Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
            }
            Proxy proxy = null;
            if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                proxy = new Proxy(null, resolutionHelper.getProxyHost(), resolutionHelper.getProxyPort(),
                        new Authentication(resolutionHelper.getProxyUsername(), resolutionHelper.getProxyPassword()));
            }

            if (StringUtils.isNotBlank(snapshotRepoUrl)) {
                logger.debug("Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
                RepositoryPolicy releasePolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                releaseRepository = new RemoteRepository("artifactory-snapshot", "default", releaseRepoUrl);
                if (authentication != null) {
                    logger.debug("Enforcing repository authentication: " + authentication + " for snapshot resolution repository");
                    releaseRepository.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("Enforcing proxy: " + proxy + " for snapshot resolution repository");
                    releaseRepository.setProxy(proxy);
                }
                releaseRepository.setPolicy(false, releasePolicy);
                releaseRepository.setPolicy(true, snapshotPolicy);
            }

            if (StringUtils.isNotBlank(releaseRepoUrl)) {
                logger.debug("Enforcing release repository for resolution: " + releaseRepoUrl);
                boolean snapshotPolicyEnabled = snapshotRepository == null;
                String repositoryId = snapshotPolicyEnabled ? "artifactory-release-snapshot" : "artifactory-release";

                RepositoryPolicy releasePolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(snapshotPolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                releaseRepository = new RemoteRepository(repositoryId, "default", releaseRepoUrl);
                if (authentication != null) {
                    logger.debug("Enforcing repository authentication: " + authentication + " for release resolution repository");
                    releaseRepository.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("Enforcing proxy: " + proxy + " for release resolution repository");
                    releaseRepository.setProxy(proxy);
                }
                releaseRepository.setPolicy(false, releasePolicy);
                releaseRepository.setPolicy(true, snapshotPolicy);
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

    private void enforceResolutionRepositories(RepositorySystemSession session, ArtifactRequest request) {
        // Get the Artifactory repositories configured in the Artifactory plugin:
        List<RemoteRepository> repositories = getResolutionRepositories(session);

        // The repositories list can be empty, in case this build is not running from a CI server.
        // In that case, we do not want to override Maven's configured repositories:
        if (repositories != null && !repositories.isEmpty()) {
            request.setRepositories(repositories);
        }
    }

    public List<ArtifactResult> resolveArtifacts( RepositorySystemSession session, Collection<? extends ArtifactRequest> requests )
            throws ArtifactResolutionException {

        for(ArtifactRequest request : requests) {
            enforceResolutionRepositories(session, request);
        }
        // Now we let Maven resolve the artifacts:
        return super.resolveArtifacts(session, requests);
    }

    public RemoteRepository getSnapshotRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        if (snapshotRepository != null) {
            return snapshotRepository;
        }
        return releaseRepository;
    }

    public RemoteRepository getReleaseRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories(session);

        return releaseRepository;
    }
}
