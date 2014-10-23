package org.jfrog.build.extractor.maven.resolver;

import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;
import org.jfrog.build.client.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.maven.Maven3BuildInfoLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Created by liorh on 4/24/14.
 */

@Component(role = ResolutionHelper.class)
public class ResolutionHelper implements Contextualizable {

    @Requirement
    private Logger logger;

    private ArtifactoryClientConfiguration internalConfiguration;
    private PlexusContainer plexusContainer;

    public void init(Properties allMavenProps) {
        if (internalConfiguration != null) {
            return;
        }

        Maven3BuildInfoLogger log = new Maven3BuildInfoLogger(logger);
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, log);
        internalConfiguration = new ArtifactoryClientConfiguration(log);
        internalConfiguration.fillFromProperties(allProps);
    }

    private void setArtifactoryEclipseArtifactResolver() throws ComponentLookupException {
        DefaultArtifactDescriptorReader descriptorReader = (DefaultArtifactDescriptorReader)plexusContainer.lookup("org.eclipse.aether.impl.ArtifactDescriptorReader");
        org.eclipse.aether.internal.impl.DefaultRepositorySystem repositorySystem = (org.eclipse.aether.internal.impl.DefaultRepositorySystem)plexusContainer.lookup("org.eclipse.aether.RepositorySystem");

        org.eclipse.aether.impl.ArtifactResolver artifactoryResolver = (org.eclipse.aether.impl.ArtifactResolver)plexusContainer.lookup("org.jfrog.build.extractor.maven.resolver.ArtifactoryEclipseArtifactResolver");

        descriptorReader.setArtifactResolver(artifactoryResolver);
        repositorySystem.setArtifactResolver(artifactoryResolver);//
    }

    private void setArtifactorySonatypeArtifactResolver() throws ComponentLookupException, InvocationTargetException, IllegalAccessException {
        DefaultArtifactDescriptorReader descriptorReader = (DefaultArtifactDescriptorReader)plexusContainer.lookup("org.sonatype.aether.impl.ArtifactDescriptorReader");
        org.sonatype.aether.impl.internal.DefaultRepositorySystem repositorySystem = (org.sonatype.aether.impl.internal.DefaultRepositorySystem)plexusContainer.lookup("org.sonatype.aether.RepositorySystem");

        org.sonatype.aether.impl.ArtifactResolver artifactoryResolver = (org.sonatype.aether.impl.ArtifactResolver)plexusContainer.lookup("org.jfrog.build.extractor.maven.resolver.ArtifactorySonatypeArtifactResolver");
        repositorySystem.setArtifactResolver(artifactoryResolver);

        // Setting the resolver. This is done using reflection, since the signature of the
        // DefaultArtifactDescriptorReader.setArtifactResolver method changed in Maven 3.1.x:
        Method setArtifactResolverMethod = null;
        Method[] methods = DefaultArtifactDescriptorReader.class.getDeclaredMethods();
        for (Method method : methods) {
            if ("setArtifactResolver".equals(method.getName())) {
                setArtifactResolverMethod = method;
                break;
            }
        }
        if (setArtifactResolverMethod == null) {
            throw new RuntimeException("Failed to enforce Artifactory resolver. Method DefaultArtifactDescriptorReader.setArtifactResolver does not exist");
        }
        setArtifactResolverMethod.invoke(descriptorReader, artifactoryResolver);
    }

    /**
     * The method replaces the DefaultArtifactResolver instance with an instance of a class extending DefaultArtifactResolver
     * (either ArtifactoryEclipseArtifactResolver or ArtifactorySonatypeArtifactResolver, depending on the Maven version being used).
     * The new extending class sets the configured Artifactory resolution repositories for each resolved artifact.
     *
     * @throws ComponentLookupException
     */
    public void enforceArtifactoryResolver() throws ComponentLookupException, InvocationTargetException, IllegalAccessException {
        if (plexusContainer.hasComponent("org.eclipse.aether.impl.ArtifactDescriptorReader")) {
            setArtifactoryEclipseArtifactResolver();
        } else
        if (plexusContainer.hasComponent("org.sonatype.aether.impl.ArtifactDescriptorReader")) {
            setArtifactorySonatypeArtifactResolver();
        } else {
            throw new RuntimeException("Could not fetch either org.eclipse.aether.impl.ArtifactDescriptorReader or org.sonatype.aether.impl.ArtifactDescriptorReader from the container");
        }
    }

    /**
     * Determines a deployed artifact's scope (either "project" or "build") according to the maven's request context sent as an argument.
     * @param requestContext    The deployed artifact's request context.
     * @return                  Scope value for the request context.
     */
    public String getScopeByRequestContext(String requestContext) {
        if (requestContext == null) {
            return "project";
        }
        if ("plugin".equals(requestContext)) {
            return "build";
        }
        return "project";
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    public String getRepoUsername() {
        return internalConfiguration.resolver.getUsername();
    }

    public String getRepoPassword() {
        return internalConfiguration.resolver.getPassword();
    }

    public String getProxyHost() {
        return internalConfiguration.proxy.getHost();
    }

    public Integer getProxyPort() {
        return internalConfiguration.proxy.getPort();
    }

    public String getProxyUsername() {
        return internalConfiguration.proxy.getUsername();
    }

    public String getProxyPassword() {
        return internalConfiguration.proxy.getPassword();
    }

    @Override
    public void contextualize(Context context) throws ContextException {
        plexusContainer = (PlexusContainer)context.get(PlexusConstants.PLEXUS_KEY);
    }
}
