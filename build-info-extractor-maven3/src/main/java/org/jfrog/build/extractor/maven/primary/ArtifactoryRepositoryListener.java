package org.jfrog.build.extractor.maven.primary;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.client.ClientProperties;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;

import java.util.Properties;

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

    private final Properties props;
    private final Logger logger;
    private final String url;
    private final String username;
    private final String password;

    public ArtifactoryRepositoryListener(Properties props, Logger logger) {
        this.props = props;
        this.logger = logger;
        this.url = props.getProperty(ClientProperties.PROP_CONTEXT_URL) + "/" +
                props.getProperty(ClientProperties.PROP_RESOLVE_REPOKEY);
        this.username = props.getProperty(ClientProperties.PROP_PUBLISH_USERNAME);
        this.password = props.getProperty(ClientProperties.PROP_PUBLISH_PASSWORD);
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
            logger.debug("Enforcing repository URL: " + url + " for event: " + event);
            ((RemoteRepository) repository).setUrl(url);
            if (StringUtils.isNotBlank(username)) {
                Authentication authentication = new Authentication(username, password);
                logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                ((RemoteRepository) repository).setAuthentication(authentication);
            }
        }
    }
}
