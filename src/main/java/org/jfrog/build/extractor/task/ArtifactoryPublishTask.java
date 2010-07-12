package org.jfrog.build.extractor.task;

import org.apache.ivy.ant.IvyPublish;
import org.apache.ivy.ant.IvyTask;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.repository.Repository;
import org.apache.ivy.plugins.repository.url.URLRepository;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.apache.ivy.util.url.CredentialsStore;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.PropertyHelper;

import java.util.List;

/**
 * A customized Ivy task that is used to configure a custom resolver which wraps around the existing resolver. It
 * extends the {@link IvyPublish} so that the artifacts will not be automatically published, but rather we have more
 * control on artifact deployment.
 *
 * @author Tomer Cohen
 */
public class ArtifactoryPublishTask extends IvyPublish {
    private static final String RESOLVER_NAME = "artifactory_resolver";
    private static final String REPOSITORY_NAME = "artifactory";
    private static final String DEFAULT_REALM = "Artifactory Realm";
    public static final String PUBLISH_ARTIFACT_TASK_NAME = "publish_artifact";

    @Override
    public void doExecute() throws BuildException {
        IvySettings ivySettings = getSettings();
        if (ivySettings.getResolver(RESOLVER_NAME) == null) {
            ivySettings.addResolver(createArtifactoryDependencyResolver(ivySettings));
        }
        Project proj = (Project) IvyContext.peekInContextStack(IvyTask.ANT_PROJECT_CONTEXT_KEY);
        List list = (List) PropertyHelper.getPropertyHelper(proj).getUserProperty("artifactory.ant.context");
        //setArtifactspattern(IBiblioResolver.DEFAULT_PATTERN);
        CredentialsStore.INSTANCE.addCredentials(DEFAULT_REALM, "", "", "");
        super.doExecute();
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
