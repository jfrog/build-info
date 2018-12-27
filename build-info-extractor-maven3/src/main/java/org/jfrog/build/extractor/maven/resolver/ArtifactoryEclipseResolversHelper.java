package org.jfrog.build.extractor.maven.resolver;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
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
import java.util.List;
import java.util.Properties;

@Named
@Component( role = ArtifactoryEclipseResolversHelper.class )
public class ArtifactoryEclipseResolversHelper {

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
