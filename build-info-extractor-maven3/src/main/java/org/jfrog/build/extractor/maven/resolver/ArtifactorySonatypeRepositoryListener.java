package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.RepositoryListener;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;

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

    private ArtifactoryClientConfiguration internalConfiguration = null;

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
        ArtifactRepository repository = event.getRepository();
        if (repository == null) {
            logger.warn("Received null repository, perhaps your maven installation is missing a settings.xml file?");
        }
        if (repository instanceof RemoteRepository) {
            ArtifactoryClientConfiguration config = getConfiguration(event.getSession());
            String repoUrl = config.resolver.getUrlWithMatrixParams();
            String repoUsername = config.resolver.getUsername();
            String repoPassword = config.resolver.getPassword();
            String proxyHost = config.proxy.getHost();
            Integer proxyPort = config.proxy.getPort();
            String proxyUsername = config.proxy.getUsername();
            String proxyPassword = config.proxy.getPassword();

            logger.debug("Enforcing repository URL: " + repoUrl + " for event: " + event);
            if (StringUtils.isBlank(repoUrl)) {
                return;
            }

            RemoteRepository remoteRepository = (RemoteRepository) repository;
            remoteRepository.setUrl(repoUrl);

            if (StringUtils.isNotBlank(repoUsername)) {
                Authentication authentication = new Authentication(repoUsername, repoPassword);
                logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                remoteRepository.setAuthentication(authentication);
            }
            if (StringUtils.isNotBlank(proxyHost)) {
                Proxy proxy = new Proxy(null, proxyHost, proxyPort, new Authentication(proxyUsername, proxyPassword));
                logger.debug("Enforcing repository proxy: " + proxy + " for event: " + event);
                remoteRepository.setProxy(proxy);
            }
        }
    }

    private ArtifactoryClientConfiguration getConfiguration(RepositorySystemSession session) {
        if (internalConfiguration != null) {
            return internalConfiguration;
        }
        Properties allMavenProps = new Properties();
        allMavenProps.putAll(session.getSystemProperties());
        allMavenProps.putAll(session.getUserProperties());

        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps);
        internalConfiguration = new ArtifactoryClientConfiguration(new Maven3BuildInfoLogger(logger));
        internalConfiguration.fillFromProperties(allProps);
        return internalConfiguration;
    }
}
