package org.jfrog.build.extractor.go.extractor;

import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author BarakH
 */
public class GoVersionUtils {

    public static final String INCOMPATIBLE = "+incompatible";
    public static final int ZERO_OR_ONE = 0;
    // The regular expression used here is derived from the SemVer specification: https://semver.org/
    protected static final Pattern VERSION_PATTERN = Pattern.compile(
            "v(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\." +
                    "(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$");

    /**
     * @param version full version string
     * @return The major version as an integer or 0 if couldn't parse it
     */
    public static int getMajorVersion(String version, Log log) {
        if (StringUtils.isEmpty(version)) {
            return 0;
        }
        version = getCleanVersion(version);
        Matcher matcher = VERSION_PATTERN.matcher(version);
        if (matcher.matches()) {
            String major = matcher.group(1);
            if (!StringUtils.isEmpty(major)) {
                try {
                    return Integer.parseInt(major);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse major version of " + version, e);
                }
            }
        }
        return 0;
    }

    /**
     * Compatible Go module from major version of 2 and above will end with /vMajor
     * github.com/owner/repo/v3 - 3
     * github.com/owner/repo/v2 - 2
     * github.com/owner/repo - 0 or 1
     *
     * @return Major version of compatible Go module
     */
    public static int getMajorProjectVersion(String project, Log log) {
        if (!StringUtils.isEmpty(project)) {
            project = project.toLowerCase();
            if (project.matches("^.*/v\\d+")) {
                String major = project.substring(project.lastIndexOf("/v") + 2);
                try {
                    return Integer.parseInt(major);
                } catch (NumberFormatException e) {
                    log.error("Failed to parse major version of " + project, e);
                }
            }
        }
        return ZERO_OR_ONE;
    }

    /**
     * @return The version string without the +incompatible part - if there is
     */
    public static String getCleanVersion(String version) {
        if (!StringUtils.isEmpty(version) && version.contains(GoVersionUtils.INCOMPATIBLE)) {
            version = version.substring(0, version.indexOf(GoVersionUtils.INCOMPATIBLE));
        }
        return version;
    }

    /**
     * From major versions of 2+, the project name must end with a /vMajor prefix (for majors of 0 and 1 it will stay without the prefix)
     * github.com/owner/repo , v2.0.5 - false
     * github.com/owner/repo , v2.0.5+incompatible - false
     * github.com/owner/repo/v2 , v2.0.5 - true
     * github.com/owner/repo , v1.0.5 - true
     * github.com/owner/repo , v0.0.5 - true
     *
     * @return True if the major version is eq or gt than 2 and the project name follows the compatible convention
     */
    public static boolean isCompatibleGoModuleNaming(String projectName, String version, Log log) {
        if (StringUtils.isBlank(projectName) || StringUtils.isBlank(version)) {
            return false;
        }
        int majorVersion = getMajorVersion(version, log);
        if (majorVersion >= 2) {
            return (projectName.endsWith("/v" + majorVersion) && !version.endsWith(INCOMPATIBLE));
        }
        return majorVersion == 1 || majorVersion == 0;
    }

    /**
     * @return Sub module name in GitHub projects
     */
    public static String getSubModule(String projectName) {
        if (StringUtils.isBlank(projectName)) {
            return StringUtils.EMPTY;
        }
        String[] parts = projectName.split("/", 4);
        if (parts.length >= 4) {
            return parts[3];
        }
        return StringUtils.EMPTY;
    }

    /**
     * @param path A file path
     * @return Parent path of the input path as if it was a file. Empty string if the path has no parent.
     */
    public static String getParent(String path) {
        if (StringUtils.isEmpty(path)) {
            return StringUtils.EMPTY;
        }
        String parentPath = new File(path).getParent();
        if (StringUtils.isNotEmpty(parentPath)) {
            return parentPath.replace('\\', '/');
        }
        return StringUtils.EMPTY;
    }
}