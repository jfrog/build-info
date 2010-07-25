package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.AbstractPatternsBasedResolver;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.URLResolver;
import org.jfrog.build.client.ClientProperties;

import java.io.File;
import java.util.List;
import java.util.Properties;


/**
 * A helper class that deals with repository paths and resolvers.
 *
 * @author Tomer Cohen
 */
public class IvyResolverHelper {
    public static final String ARTIFACTORY_RESOLVER_NAME = "publish_artifactory";
    public static final String DEFAULT_PATTERN = "[organization]/[module]/[revision]/[artifact]-[revision].[ext]";

    /**
     * Calculate a repo path for a file
     *
     * @param props
     * @param artifactFile The file to be deployed.
     * @param organisation The module's organization.
     * @param module       The module's name.
     * @param revision     The module's revision.     @return A calculated repository path to where the file should be
     *                     published to.
     */
    public static String calculateArtifactPath(Properties props, File artifactFile, String organisation, String module,
            String revision) {
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        boolean m2Compatible = true;
        DependencyResolver resolver = ivySettings.getResolver(ARTIFACTORY_RESOLVER_NAME);
        if (resolver != null) {
            URLResolver artifactoryResolver = (URLResolver) resolver;
            m2Compatible = artifactoryResolver.isM2compatible();
        }
        ModuleRevisionId mrid;
        if (m2Compatible) {
            mrid = ModuleRevisionId.newInstance(organisation.replace(".", "/"), module, revision);
        } else {
            mrid = ModuleRevisionId.newInstance(organisation, module, revision);
        }
        String fullPattern;
        if (artifactFile.getAbsolutePath().endsWith(".jar")) {
            String pattern = getArtifactPatternFromIvy(props);
            String moduleName = artifactFile.getName().substring(0, artifactFile.getName().indexOf("."));
            fullPattern = IvyPatternHelper.substitute(pattern, mrid, moduleName, "jar", "jar");
        } else {
            String pattern = getIvyPatternFromIvy(props);
            fullPattern = IvyPatternHelper.substitute(pattern, mrid);
        }
        int index = fullPattern.indexOf(mrid.getOrganisation());
        if (index != -1) {
            fullPattern = fullPattern.substring(index);
        } else {
            fullPattern =
                    StringUtils.removeStart(fullPattern.substring(fullPattern.indexOf(getTargetRepository(props))),
                            getTargetRepository(props));
        }
        return fullPattern;
    }

    /**
     * Get the target repository from the resolver pattern.
     *
     * @return The target repository from the resolver pattern.
     */
    public static String getTargetRepository(Properties props) {
        String patternFromIvy = getArtifactPatternFromIvy(props);
        int indexOfArtifactoryRoot = StringUtils.indexOf(patternFromIvy, "artifactory/");
        if (indexOfArtifactoryRoot != -1) {
            String repository = StringUtils.substringBetween(patternFromIvy, "artifactory/", "/");
            if (StringUtils.isNotBlank(repository)) {
                return repository;
            }
        }
        return props.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, "");
    }

    private static String getArtifactPatternFromIvy(Properties props) {
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        DependencyResolver publishingResolver = ivySettings.getResolver(ARTIFACTORY_RESOLVER_NAME);
        if (publishingResolver != null) {
            AbstractPatternsBasedResolver urlResolver = (AbstractPatternsBasedResolver) publishingResolver;
            List artifactPatterns = urlResolver.getArtifactPatterns();
            if (!artifactPatterns.isEmpty()) {
                return artifactPatterns.get(0).toString();
            }
        }
        return getDefaultPattern(props);
    }

    private static String getIvyPatternFromIvy(Properties props) {
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        DependencyResolver publishingResolver = ivySettings.getResolver(ARTIFACTORY_RESOLVER_NAME);
        if (publishingResolver != null) {
            AbstractPatternsBasedResolver urlResolver = (AbstractPatternsBasedResolver) publishingResolver;
            List ivyPatterns = urlResolver.getIvyPatterns();
            if (!ivyPatterns.isEmpty()) {
                return ivyPatterns.get(0).toString();
            }
        }
        return getDefaultPattern(props);
    }

    private static String getDefaultPattern(Properties props) {
        String contextUrl = props.getProperty(ClientProperties.PROP_CONTEXT_URL, "");
        contextUrl = StringUtils.stripEnd(contextUrl, "/");
        String repoKey = props.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, "");
        repoKey = StringUtils.stripEnd(repoKey, "/");
        return contextUrl + "/" + repoKey + "/" + DEFAULT_PATTERN;
    }
}
