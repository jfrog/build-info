package org.jfrog.build.extractor.maven.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.repository.RepositoryPolicy;

import javax.inject.Named;
import java.util.List;
import java.util.Properties;

@Named
@Component( role = ArtifactorySonatypeResolversHelper.class )
public class ArtifactorySonatypeResolversHelper {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Requirement
    private Logger logger;

    private List<RemoteRepository> resolutionRepositories = null;
    private RemoteRepository releaseRepository = null;
    private RemoteRepository snapshotRepository = null;

    public void initResolutionRepositories(RepositorySystemSession session) {
        getResolutionRepositories(session);
    }

    /**
     * Create a list containing one release and one snapshot resolution repositories, according to the configuration in the Artifactory plugin.
     * The list is used to override Maven's default or configured repositories, so that the build dependencies are resolved from Artifactory.
     * The list is saved and reused for further invokations to this method.
     * @param session
     * @return
     */
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
                RepositoryPolicy releasePolicy = new RepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotPolicy = new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                snapshotRepository = new RemoteRepository("artifactory-snapshot", "default", releaseRepoUrl);
                if (authentication != null) {
                    logger.debug("Enforcing repository authentication: " + authentication + " for snapshot resolution repository");
                    snapshotRepository.setAuthentication(authentication);
                }
                if (proxy != null) {
                    logger.debug("Enforcing proxy: " + proxy + " for snapshot resolution repository");
                    snapshotRepository.setProxy(proxy);
                }

                snapshotRepository.setPolicy(false, releasePolicy);
                snapshotRepository.setPolicy(true, snapshotPolicy);
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