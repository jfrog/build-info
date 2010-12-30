package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.apache.ivy.plugins.resolver.IBiblioResolver;
import org.jfrog.build.client.ClientIvyProperties;
import org.jfrog.build.client.ClientProperties;
import org.jfrog.build.client.LayoutPatterns;

import java.io.File;
import java.util.Map;
import java.util.Properties;


/**
 * A helper class that deals with repository paths and resolvers.
 *
 * @author Tomer Cohen
 */
public class IvyResolverHelper {
    private static final String IVY_XML = "ivy.xml";


    /**
     * Calculate a repo path for a file
     *
     * @param props
     * @param artifactFile    The file to be deployed.
     * @param extraAttributes
     */
    public static String calculateArtifactPath(Properties props, File artifactFile, Map<String, String> attributes,
            Map<String, String> extraAttributes) {
        String organization = attributes.get("organisation");
        String revision = attributes.get("revision");
        String moduleName = attributes.get("module");
        String ext = attributes.get("ext");
        String type = attributes.get("type");
        String branch = attributes.get("branch");
        String artifactPattern = getPattern(props, artifactFile.getName());
        return IvyPatternHelper.substitute(artifactPattern, getGroupIdPatternByM2Compatible(props, organization),
                moduleName, revision, null, type, ext, branch, extraAttributes, null);
    }

    private static String getExt(String path) {
        int dot = path.lastIndexOf('.');
        return path.substring(dot + 1);
    }

    private static String getPattern(Properties props, String fileName) {
        if (isIvyFileName(fileName)) {
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
            if (isM2Compatible(props)) {
                pattern = LayoutPatterns.M2_PATTERN;
            } else {
                pattern = IBiblioResolver.DEFAULT_PATTERN;
            }
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

    public static boolean isIvyFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        return IVY_XML.equals(fileName) || (fileName.startsWith("ivy-") && fileName.endsWith(".xml")) ||
                fileName.endsWith(".ivy") ||
                fileName.endsWith("-" + IVY_XML);
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
