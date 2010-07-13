package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyContext;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.settings.IvySettings;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.apache.ivy.plugins.resolver.URLResolver;

import java.io.File;


/**
 * A helper class that deals with repository paths and resolvers.
 *
 * @author Tomer Cohen
 */
public class IvyResolverHelper {

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
        boolean m2Compatible = false;
        DependencyResolver resolver = ivySettings.getResolver("publish_artifactory");
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
        if (artifactFile.getAbsolutePath().endsWith(".jar")) {
            String fullPattern = IvyPatternHelper.substitute(pattern, mrid, artifactFile.getName(), "jar", "jar");
            fullPattern = fullPattern.substring(fullPattern.indexOf(mrid.getOrganisation()));
            return fullPattern;
        } else {
            String fullPattern = IvyPatternHelper.substitute(pattern, mrid);
            fullPattern = fullPattern.substring(fullPattern.indexOf(mrid.getOrganisation()));
            return fullPattern;
        }
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
        String artifactPattern =
                "http://repo.jfrog.org/artifactory/libs-releases-local/" + IBiblioResolver.DEFAULT_PATTERN;
        DependencyResolver publishingResolver = ivySettings.getResolver("publish_artifactory");
        if (publishingResolver != null) {
            URLResolver urlResolver = (URLResolver) publishingResolver;
            artifactPattern = urlResolver.getArtifactPatterns().get(0).toString();
        }
        return artifactPattern;
    }
}
