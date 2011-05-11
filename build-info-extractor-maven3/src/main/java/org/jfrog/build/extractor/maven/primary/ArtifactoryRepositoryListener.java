package org.jfrog.build.extractor.maven.primary;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.Proxy;
import org.sonatype.aether.repository.RemoteRepository;

/**
 * A repository listener that is used for interception of repository events (resolution and deployment) The repository
 * URL and authentication will chance according to what is given from the build server or command line system
 * properties.
 * <p/>
 * The listener is registered in {@link org.jfrog.build.extractor.maven.BuildInfoRecorderLifecycleParticipant}
 *
 * @author Tomer Cohen
 */
public class ArtifactoryRepositoryListener extends AbstractRepositoryListener {

    private final ArtifactoryClientConfiguration clientConf;
    private final Logger logger;

    public ArtifactoryRepositoryListener(ArtifactoryClientConfiguration clientConf, Logger logger) {
        this.clientConf = clientConf;
        this.logger = logger;
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        logger.debug("Intercepted artifact downloading event: " + event);
        enforceRepository(event);
        super.artifactDownloading(event);
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        logger.debug("Intercepted metadata downloading event: " + event);
        enforceRepository(event);
        super.metadataDownloading(event);
    }

    private void enforceRepository(RepositoryEvent event) {
        ArtifactRepository repository = event.getRepository();
        if (repository instanceof RemoteRepository) {
            ArtifactoryClientConfiguration.ResolverHandler resolverHandler = clientConf.resolver;
            String url = resolverHandler.getUrlWithMatrixParams();
            logger.debug("Enforcing repository URL: " + url + " for event: " + event);
            if (StringUtils.isBlank(url)) {
                return;
            }
            ((RemoteRepository) repository).setUrl(url);
            if (StringUtils.isNotBlank(resolverHandler.getUsername())) {
                Authentication authentication = new Authentication(resolverHandler.getUsername(), resolverHandler.getPassword());
                logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                ((RemoteRepository) repository).setAuthentication(authentication);
            }
            ArtifactoryClientConfiguration.ProxyHandler proxyHandler = clientConf.proxy;
            if (StringUtils.isNotBlank(proxyHandler.getHost())) {
                Proxy proxy = new Proxy(null, proxyHandler.getHost(), proxyHandler.getPort(), new Authentication(proxyHandler.getUsername(), proxyHandler.getPassword()));
                ((RemoteRepository) repository).setProxy(proxy);
            }
        }
    }
}
