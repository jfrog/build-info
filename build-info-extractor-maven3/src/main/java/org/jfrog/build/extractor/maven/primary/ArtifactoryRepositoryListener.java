package org.jfrog.build.extractor.maven.primary;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.ArtifactoryResolutionProperties;
import org.jfrog.build.client.ClientProperties;
import org.sonatype.aether.AbstractRepositoryListener;
import org.sonatype.aether.RepositoryEvent;
import org.sonatype.aether.repository.ArtifactRepository;
import org.sonatype.aether.repository.Authentication;
import org.sonatype.aether.repository.RemoteRepository;

import java.util.Map;
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
    private final String matrixParams;

    public ArtifactoryRepositoryListener(Properties props, Logger logger) {
        this.props = props;
        this.logger = logger;
        this.url = props.getProperty(ClientProperties.PROP_CONTEXT_URL) + "/" +
                props.getProperty(ClientProperties.PROP_RESOLVE_REPOKEY);
        this.username = props.getProperty(ClientProperties.PROP_RESOLVE_USERNAME);
        this.password = props.getProperty(ClientProperties.PROP_RESOLVE_PASSWORD);
        this.matrixParams = getMatrixParams();
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
            ((RemoteRepository) repository).setUrl(url + matrixParams);
            if (StringUtils.isNotBlank(username)) {
                Authentication authentication = new Authentication(username, password);
                logger.debug("Enforcing repository authentication: " + authentication + " for event: " + event);
                ((RemoteRepository) repository).setAuthentication(authentication);
            }
        }
    }

    private String getMatrixParams() {
        StringBuilder builder = new StringBuilder();
        ImmutableMap<String, String> map = Maps.fromProperties(props);
        Map<String, String> filtered = Maps.filterKeys(map, new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.startsWith(ArtifactoryResolutionProperties.ARTIFACT_BUILD_ROOT_KEY);
            }
        });
        for (Map.Entry<String, String> entry : filtered.entrySet()) {
            builder.append(";").append(ArtifactoryResolutionProperties.ARTIFACTORY_BUILD_ROOT_MATRIX_PARAM_KEY)
                    .append(StringUtils
                            .removeStart(entry.getKey(), ArtifactoryResolutionProperties.ARTIFACT_BUILD_ROOT_KEY))
                    .append("=").append(entry.getValue());
        }
        if (!filtered.isEmpty()) {
            builder.append(";");
        }
        return builder.toString();
    }
}
