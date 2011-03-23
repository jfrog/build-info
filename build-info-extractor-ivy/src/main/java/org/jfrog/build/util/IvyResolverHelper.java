package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.jfrog.build.client.ClientIvyProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.LayoutPatterns;

import java.util.Map;
import java.util.Properties;


/**
 * A helper class that deals with repository paths and resolvers.
 *
 * @author Tomer Cohen
 */
public class IvyResolverHelper {

    /**
     * Calculate a repo path for a file
     *
     * @param props
     * @param extraAttributes
     */
    public static String calculateArtifactPath(Properties props, Map<String, String> attributes,
            Map<String, String> extraAttributes) {
        String organization = attributes.get("organisation");
        String revision = attributes.get("revision");
        String moduleName = attributes.get("module");
        String ext = attributes.get("ext");
        String branch = attributes.get("branch");
        String type = attributes.get("type");
        String artifactPattern = getPattern(props, type);
        String orgPattern = getGroupIdPatternByM2Compatible(props, organization);
        return IvyPatternHelper.substitute(artifactPattern, orgPattern,
                moduleName, branch, revision, attributes.get("artifact"), type, ext, attributes.get("conf"), null,
                extraAttributes, null);
    }

    private static String getPattern(Properties props, String type) {
        if (isIvy(type)) {
            return getIvyDescriptorPattern(props);
        } else {
            return getArtifactPattern(props);
        }
    }

    public static String getIvyDescriptorPattern(Properties props) {
        String pattern = props.getProperty(ClientIvyProperties.PROP_IVY_IVY_PATTERN);
        if (StringUtils.isNotBlank(pattern)) {
            return pattern.trim();
        }
        return LayoutPatterns.DEFAULT_IVY_PATTERN;
    }

    private static String getArtifactPattern(Properties props) {
        String pattern = props.getProperty(ClientIvyProperties.PROP_IVY_ARTIFACT_PATTERN);
        if (StringUtils.isBlank(pattern)) {
            pattern = LayoutPatterns.M2_PATTERN;
        }
        return pattern.trim();
    }

    private static String getGroupIdPatternByM2Compatible(Properties props, String groupId) {
        if (isM2Compatible(props)) {
            groupId = groupId.replace(".", "/");
        }
        return groupId;
    }

    private static boolean isM2Compatible(Properties props) {
        String m2Compatible = props.getProperty(ClientIvyProperties.PROP_M2_COMPATIBLE);
        return Boolean.parseBoolean(m2Compatible);
    }

    public static boolean isIvy(String type) {
        if (StringUtils.isBlank(type)) {
            return false;
        }
        return "ivy".equals(type);
    }

    /**
     * Get the target repository from the resolver pattern.
     *
     * @return The target repository from the resolver pattern.
     */
    public static String getTargetRepository(Properties props) {
        String targetRepository = props.getProperty(ClientProperties.PROP_PUBLISH_REPOKEY, "");
        if (StringUtils.isBlank(targetRepository)) {
            throw new IllegalArgumentException("Publishing repository key is blank");
        }
        return targetRepository;
    }

    public static String getClassifier(String artifactName) {
        int index = artifactName.indexOf('-');
        if (index == -1) {
            return "";
        }
        String substring = artifactName.substring(artifactName.indexOf('-') + 1);
        int dot = substring.indexOf('.');
        if (dot == -1) {
            return "";
        }
        return substring.substring(0, dot);
    }
}
