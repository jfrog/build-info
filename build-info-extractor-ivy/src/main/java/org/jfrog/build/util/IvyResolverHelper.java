package org.jfrog.build.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Map;


/**
 * A helper class that deals with repository paths and resolvers.
 *
 * @author Tomer Cohen
 */
public class IvyResolverHelper {

    /**
     * Calculate a repo path for a file
     *
     * @param publisher
     * @param attributes
     * @param extraAttributes
     */
    public static String calculateArtifactPath(ArtifactoryClientConfiguration.PublisherHandler publisher,
            Map<String, String> attributes,
            Map<String, String> extraAttributes) {
        String organization = attributes.get("organisation");
        String revision = attributes.get("revision");
        String moduleName = attributes.get("module");
        String ext = attributes.get("ext");
        String branch = attributes.get("branch");
        String type = attributes.get("type");
        String artifactPattern = getPattern(publisher, type);
        if (publisher.isM2Compatible()) {
            organization = organization.replace(".", "/");
        }
        return IvyPatternHelper.substitute(artifactPattern, organization,
                moduleName, branch, revision, attributes.get("artifact"), type, ext, attributes.get("conf"), null,
                extraAttributes, null);
    }

    private static String getPattern(ArtifactoryClientConfiguration.PublisherHandler pub, String type) {
        if (isIvy(type)) {
            return pub.getIvyPattern();
        } else {
            return pub.getIvyArtifactPattern();
        }
    }

    private static boolean isIvy(String type) {
        if (StringUtils.isBlank(type)) {
            return false;
        }
        return "ivy".equals(type);
    }
}
