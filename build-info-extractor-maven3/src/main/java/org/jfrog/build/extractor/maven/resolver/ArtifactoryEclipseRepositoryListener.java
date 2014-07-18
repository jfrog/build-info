package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.util.repository.AuthenticationBuilder;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Repository listener for enforcing resolution from Artifactory when running in Maven 3.1.x
 *
 * @author Shay Yaakov
 */
@Component(role = RepositoryListener.class)
public class ArtifactoryEclipseRepositoryListener extends AbstractRepositoryListener {

    @Requirement
    private Logger logger;

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Eclipse repository artifact downloading event: " + event);
        enforceRepository(event);
        super.artifactDownloading(event);
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Eclipse repository metadata downloading event: " + event);
        enforceRepository(event);
        super.metadataDownloading(event);
    }

    private void enforceRepository(RepositoryEvent event) {
        ArtifactRepository repository = event.getRepository();
        if (repository == null) {
            logger.warn("Received null repository, perhaps your maven installation is missing a settings.xml file?");
        }
        if (repository instanceof RemoteRepository) {
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(event.getSession().getSystemProperties());
            allMavenProps.putAll(event.getSession().getUserProperties());

            resolutionHelper.resolve(allMavenProps, logger);

            ResolutionHelper.Nature resolutionType = resolutionType(event);
            String enforcingRepository = resolutionHelper.getEnforceRepository(resolutionType);
            if (StringUtils.isBlank(enforcingRepository)) {
                return;
            }

            logger.debug("Enforcing repository URL: " + enforcingRepository + " for event: " + event);
            RemoteRepository remoteRepository = (RemoteRepository) repository;

            try {
                Field url = RemoteRepository.class.getDeclaredField("url");
                url.setAccessible(true);
                url.set(remoteRepository, enforcingRepository);

                if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                    Authentication authentication = new AuthenticationBuilder()
                            .addString("username", resolutionHelper.getRepoUsername()).addSecret("password", resolutionHelper.getRepoPassword()).build();
                    logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                    Field authenticationField = RemoteRepository.class.getDeclaredField("authentication");
                    authenticationField.setAccessible(true);
                    authenticationField.set(remoteRepository, authentication);
                }

                logger.debug("Enforcing snapshot and release policy for event: " + event);

                boolean isSnapshot = resolutionType.equals(ResolutionHelper.Nature.SNAPSHOT);
                RepositoryPolicy releasePolicy = new RepositoryPolicy(!isSnapshot, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
                RepositoryPolicy snapshotolicy = new RepositoryPolicy(isSnapshot, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);

                Field releasePolicyField = RemoteRepository.class.getDeclaredField("releasePolicy");
                releasePolicyField.setAccessible(true);
                releasePolicyField.set(remoteRepository, releasePolicy);
                Field snapshotPolicyField = RemoteRepository.class.getDeclaredField("snapshotPolicy");
                snapshotPolicyField.setAccessible(true);
                snapshotPolicyField.set(remoteRepository, snapshotolicy);

                if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                    Authentication authentication = new AuthenticationBuilder()
                            .addString("username", resolutionHelper.getProxyUsername()).addSecret("password", resolutionHelper.getProxyPassword()).build();
                    Proxy proxy = new Proxy(null, resolutionHelper.getProxyHost(), resolutionHelper.getProxyPort(), authentication);
                    logger.debug("Enforcing repository proxy: " + proxy + " for event: " + event);
                    Field proxyField = RemoteRepository.class.getDeclaredField("proxy");
                    proxyField.setAccessible(true);
                    proxyField.set(remoteRepository, proxy);
                }
            } catch (Exception e) {
                logger.error("Failed enforcing repository: " + e.getMessage(), e);
            }
        }
    }

    public ResolutionHelper.Nature resolutionType(RepositoryEvent event) {
        /*Check if we are downloading metadata or artifact*/
        if (event.getArtifact() == null) {
            boolean isSnapshot = event.getMetadata().getNature().compareTo(Metadata.Nature.SNAPSHOT) == 0;
            if (isSnapshot) {
                return ResolutionHelper.Nature.SNAPSHOT;
            }

            return ResolutionHelper.Nature.RELEASE;
        }

        /*Check if we are downloading metadata or artifact*/
        if (event.getMetadata() == null) {
            Artifact currentArtifact = event.getArtifact();
            if (currentArtifact.isSnapshot()) {
                return ResolutionHelper.Nature.SNAPSHOT;
            }

            return ResolutionHelper.Nature.RELEASE;
        }

        return null;
    }
}
