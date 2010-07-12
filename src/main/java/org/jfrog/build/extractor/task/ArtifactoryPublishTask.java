package org.jfrog.build.extractor.task;

import org.apache.ivy.ant.IvyDeliver;
import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.tools.ant.BuildException;

/**
 * A customized Ivy task that is used to configure a custom resolver which wraps around the existing resolver. It
 * extends the {@link IvyPublish} so that the artifacts will not be automatically published, but rather we have more
 * control on artifact deployment.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPublishTask extends IvyDeliver {
    private static final String RESOLVER_NAME = "artifactory_resolver";
    private static final String REPOSITORY_NAME = "artifactory";
    public static final String PUBLISH_ARTIFACT_TASK_NAME = "publish_artifact";

    @Override
    public void doExecute() throws BuildException {
        IvyContext ivyContext = IvyContext.getContext();
        IvySettings ivySettings = ivyContext.getSettings();
        if (ivySettings.getResolver(RESOLVER_NAME) == null) {
            ivySettings.addResolver(createArtifactoryDependencyResolver(ivySettings));
        }
        setDeliverpattern(IBiblioResolver.DEFAULT_PATTERN);
    }

    private DependencyResolver createArtifactoryDependencyResolver(IvySettings ivySettings) {
        URLResolver artifactoryResolver = new URLResolver();
        artifactoryResolver.setName(RESOLVER_NAME);
        artifactoryResolver.setM2compatible(true);
        artifactoryResolver.setRepository(createArtifactoryRepository());
        artifactoryResolver.setSettings(ivySettings);
        return artifactoryResolver;
    }

    private Repository createArtifactoryRepository() {
        URLRepository repository = new URLRepository();
        repository.setName(REPOSITORY_NAME);
        return repository;
    }
}
