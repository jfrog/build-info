package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.repository.*;

import java.util.Properties;

/**
 * Repository listener for enforcing resolution from Artifactory when running in Maven 3.0.x
 *
 * @author Shay Yaakov
 */
@Component(role = RepositoryListener.class)
public class ArtifactorySonatypeRepositoryListener extends AbstractRepositoryListener {

    @Requirement
    private Logger logger;

    //@Requirement(role = ResolutionHelper.class, hint = "default", optional = false)
    private ResolutionHelper resolutionHelper;

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Sonatype repository artifact downloading event: " + event);
        enforceRepository(event);
        super.artifactDownloading(event);
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        logger.debug("Intercepted Sonatype repository metadata downloading event: " + event);
        enforceRepository(event);
        super.metadataDownloading(event);
    }

    private void enforceRepository(RepositoryEvent event) {
        resolutionHelper = getResolver();
        ArtifactRepository repository = event.getRepository();
        if (repository == null) {
            logger.warn("Received null repository, perhaps your maven installation is missing a settings.xml file?");
        }
        if (repository instanceof RemoteRepository) {
            //ResolutionHelper resolutionHelper = new ResolutionHelper(event,logger);
            Properties allMavenProps = new Properties();
            allMavenProps.putAll(event.getSession().getSystemProperties());
            allMavenProps.putAll(event.getSession().getUserProperties());

            resolutionHelper.resolve(allMavenProps, logger);

            String enforcingRepository = resolutionHelper.getEnforceRepository(event);
            if (StringUtils.isBlank(enforcingRepository)) {
                return;
            }

            logger.debug("Enforcing repository URL: " + enforcingRepository + " for event: " + event);

            RemoteRepository remoteRepository = (RemoteRepository) repository;
            remoteRepository.setUrl(enforcingRepository);

            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                Authentication authentication = new Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
                logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                remoteRepository.setAuthentication(authentication);
            }

            logger.debug("Enforcing snapshot and release policy for event: " + event);
            remoteRepository.setPolicy(true, new RepositoryPolicy());
            remoteRepository.setPolicy(false, new RepositoryPolicy());

            if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                Proxy proxy = new Proxy(null, resolutionHelper.getProxyHost(), resolutionHelper.getProxyPort(),
                        new Authentication(resolutionHelper.getProxyUsername(), resolutionHelper.getProxyPassword()));
                logger.debug("Enforcing repository proxy: " + proxy + " for event: " + event);
                remoteRepository.setProxy(proxy);
            }
        }
    }

    public ResolutionHelper getResolver() {
        if (resolutionHelper != null)
            return resolutionHelper;

        return new ResolutionHelper();
    }
}
