package org.jfrog.build.extractor.maven.primary

import org.apache.commons.lang.StringUtils
import org.codehaus.plexus.logging.Logger
import org.eclipse.aether.AbstractRepositoryListener
import org.eclipse.aether.RepositoryEvent
import org.eclipse.aether.repository.ArtifactRepository
import org.eclipse.aether.repository.Authentication
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.util.repository.SecretAuthentication
import org.jfrog.build.client.ArtifactoryClientConfiguration


class ArtifactoryRepositoryListener extends AbstractRepositoryListener {

    private final ArtifactoryClientConfiguration clientConf
    private final Logger logger

    public ArtifactoryRepositoryListener ( ArtifactoryClientConfiguration clientConf, Logger logger ) {
        this.clientConf = clientConf
        this.logger     = logger
    }

    @Override
    public void artifactDownloading(RepositoryEvent event) {
        logger.debug("Intercepted artifact downloading event: " + event)
        enforceRepository(event)
        super.artifactDownloading(event)
    }

    @Override
    public void metadataDownloading(RepositoryEvent event) {
        logger.debug("Intercepted metadata downloading event: " + event)
        enforceRepository(event)
        super.metadataDownloading(event)
    }

    @SuppressWarnings([ 'GroovyAccessibility' ])
    private void enforceRepository(RepositoryEvent event) {
        ArtifactRepository repository = event.repository

        if ( repository instanceof RemoteRepository ){

            final remoteRepository = ( RemoteRepository ) repository
            final resolverHandler  = clientConf.resolver
            final proxyHandler     = clientConf.proxy
            final url              = resolverHandler.urlWithMatrixParams

            logger.debug( "Enforcing repository URL: $url for event: $event" )

            if ( StringUtils.isBlank( url )) { return }

            remoteRepository.url = url

            if ( StringUtils.isNotBlank( resolverHandler.username )) {
                Authentication authentication = new SecretAuthentication( resolverHandler.username , resolverHandler.password )
                logger.debug( "Enforcing repository authentication: $authentication for event: $event" )
                remoteRepository.authentication = authentication
            }

            if ( StringUtils.isNotBlank( proxyHandler.host )) {
                org.eclipse.aether.repository.Proxy proxy =
                    new org.eclipse.aether.repository.Proxy( null, proxyHandler.host, proxyHandler.port,
                                                             new SecretAuthentication( proxyHandler.username , proxyHandler.password ))
                remoteRepository.proxy = proxy
            }
        }
    }
}
