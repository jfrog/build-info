package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.URLResolver;

import java.io.File;


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
     * @param artifactFile The file to be deployed.
     * @param organisation The module's organization.
     * @param module       The module's name.
     * @param revision     The module's revision.
     * @return A calculated repository path to where the file should be published to.
     */
    public static String calculateArtifactPath(File artifactFile, String organisation, String module, String revision) {
        String pattern = getArtifactPatternFromIvy();
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
            String moduleName = artifactFile.getName().substring(0, artifactFile.getName().indexOf("."));
            fullPattern = IvyPatternHelper.substitute(pattern, mrid, moduleName, "jar", "jar");
        } else {
            fullPattern = IvyPatternHelper.substitute(pattern, mrid);
        }
        int index = fullPattern.indexOf(mrid.getOrganisation());
        if (index != -1) {
            fullPattern = fullPattern.substring(index);
        } else {
            fullPattern =
                    StringUtils.removeStart(fullPattern.substring(fullPattern.indexOf(getTargetRepository())),
                            getTargetRepository());
        }
        return fullPattern;
    }

    /**
     * Get the target repository from the resolver pattern.
     *
     * @return The target repository from the resolver pattern.
     */
    public static String getTargetRepository() {
        String patternFromIvy = getArtifactPatternFromIvy();
        int indexOfArtifactoryRoot = StringUtils.indexOf(patternFromIvy, "artifactory/");
        if (indexOfArtifactoryRoot != -1) {
            String repository = StringUtils.substringBetween(patternFromIvy, "artifactory/", "/");
            if (StringUtils.isNotBlank(repository)) {
                return repository;
            }
        }
        return "";
    }

    private static String getArtifactPatternFromIvy() {
        IvyContext context = IvyContext.getContext();
        IvySettings ivySettings = context.getSettings();
        DependencyResolver publishingResolver = ivySettings.getResolver(ARTIFACTORY_RESOLVER_NAME);
        if (publishingResolver != null) {
            URLResolver urlResolver = (URLResolver) publishingResolver;
            return urlResolver.getArtifactPatterns().get(0).toString();
        }
        return "http://localhost:8080/artifactory/libs-releases-local/" + DEFAULT_PATTERN;
    }
}
