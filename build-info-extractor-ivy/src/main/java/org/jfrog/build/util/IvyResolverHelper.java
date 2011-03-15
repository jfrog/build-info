package org.jfrog.build.util;

import org.apache.commons.lang.StringUtils;
import org.apache.ivy.core.IvyPatternHelper;
import org.jfrog.build.client.ArtifactoryClientConfiguration;

import java.io.File;
import java.util.Map;


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
     * @param artifactFile    The file to be deployed.
     * @param extraAttributes
     */
    public static String calculateArtifactPath(ArtifactoryClientConfiguration.PublisherHandler publisher,
            File artifactFile, Map<String, String> attributes,
            Map<String, String> extraAttributes) {
        String organization = attributes.get("organisation");
        String revision = attributes.get("revision");
        String moduleName = attributes.get("module");
        String ext = attributes.get("ext");
        String type = attributes.get("type");
        String branch = attributes.get("branch");
        String artifactPattern = getPattern(publisher, artifactFile.getName());
        if (publisher.isM2Compatible()) {
            organization = organization.replace(".", "/");
        }
        return IvyPatternHelper.substitute(artifactPattern, organization,
                moduleName, revision, attributes.get("artifact"), type, ext, branch, extraAttributes, null);
    }

    private static String getPattern(ArtifactoryClientConfiguration.PublisherHandler props, String fileName) {
        if (isIvyFileName(fileName)) {
            return props.getIvyPattern();
        } else {
            return props.getIvyArtifactPattern();
        }
    }

    public static boolean isIvyFileName(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        return IVY_XML.equals(fileName) || (fileName.startsWith("ivy-") && fileName.endsWith(".xml")) ||
                fileName.endsWith(".ivy") ||
                fileName.endsWith("-" + IVY_XML);
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
