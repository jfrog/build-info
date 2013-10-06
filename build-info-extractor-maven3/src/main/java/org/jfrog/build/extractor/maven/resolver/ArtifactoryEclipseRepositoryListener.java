package org.jfrog.build.extractor.maven.resolver;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.RepositoryListener;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.repository.Authentication;
import org.eclipse.aether.repository.Proxy;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.repository.AuthenticationBuilder;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;

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

    private ArtifactoryClientConfiguration internalConfiguration = null;

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
            logger.debug("Received null repository, perhaps your maven installation is missing a settings.xml file?");
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

            try {
                Field url = RemoteRepository.class.getDeclaredField("url");
                url.setAccessible(true);
                url.set(remoteRepository, repoUrl);

                if (StringUtils.isNotBlank(repoUsername)) {
                    Authentication authentication = new AuthenticationBuilder().addSecret(repoUsername, repoPassword).build();
                    logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                    Field authenticationField = RemoteRepository.class.getDeclaredField("authentication");
                    authenticationField.setAccessible(true);
                    authenticationField.set(remoteRepository, authentication);
                }
                if (StringUtils.isNotBlank(proxyHost)) {
                    Authentication authentication = new AuthenticationBuilder().addSecret(proxyUsername, proxyPassword).build();
                    Proxy proxy = new Proxy(null, proxyHost, proxyPort, authentication);
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
