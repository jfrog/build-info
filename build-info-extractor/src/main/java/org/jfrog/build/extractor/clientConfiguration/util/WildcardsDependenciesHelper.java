package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by Tamirh on 25/04/2016.
 */
public class WildcardsDependenciesHelper implements DependenciesHelper {
    private DependenciesDownloader downloader;
    private Log log;
    private String artifactoryUrl;
    private String target;
    private String props;
    private String buildName;
    private String buildNumber;
    private boolean recursive;

    public WildcardsDependenciesHelper(DependenciesDownloader downloader, String target, Log log) {
        this.downloader = downloader;
        this.log = log;
        this.artifactoryUrl = downloader.getClient().getArtifactoryUrl();
        this.target = target;
        this.recursive = false;
        this.props = "";
        this.buildName = "";
        this.buildNumber = "";
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public void setProps(String props) {
        this.props = StringUtils.defaultIfEmpty(props , "");
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = StringUtils.defaultIfEmpty(buildName , "");
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = StringUtils.defaultIfEmpty(buildNumber , "");
    }

    @Override
    public List<Dependency> retrievePublishedDependencies(String searchPattern, String[] excludePatterns, boolean explode) throws IOException {
        if (StringUtils.isBlank(searchPattern)) {
            return Collections.emptyList();
        }
        AqlDependenciesHelper dependenciesHelper = new AqlDependenciesHelper(downloader, target, log);
        if (StringUtils.isNotBlank(buildName)) {
            dependenciesHelper.setBuildName(buildName);
            dependenciesHelper.setBuildNumber(buildNumber);
        }
        Set<DownloadableArtifact> downloadableArtifacts = dependenciesHelper.collectArtifactsToDownload(buildAqlSearchQuery(searchPattern, excludePatterns, this.recursive, this.props), explode);
        replaceTargetPlaceholders(searchPattern, downloadableArtifacts);
        return dependenciesHelper.downloadDependencies(downloadableArtifacts);
    }

    private void replaceTargetPlaceholders(String searchPattern, Set<DownloadableArtifact> downloadableArtifacts) {
        Pattern pattern = Pattern.compile(PathsUtils.pathToRegExp(searchPattern));
        target = StringUtils.defaultIfEmpty(target , "");
        for (DownloadableArtifact artifact : downloadableArtifacts) {
            String repoName = StringUtils.substringAfterLast(artifact.getRepoUrl(), "/");
            if (StringUtils.isEmpty(target) || target.endsWith("/")) {
                artifact.setTargetDirPath(PathsUtils.reformatRegexp(repoName + "/" + artifact.getFilePath(),
                        target, pattern));
            } else {
                String targetAfterReplacement = PathsUtils.reformatRegexp(repoName + "/" + artifact.getFilePath(),
                        target, pattern);
                Map<String, String> targetFileName = PathsUtils.replaceFilesName(targetAfterReplacement, artifact.getRelativeDirPath());
                artifact.setRelativeDirPath(targetFileName.get("srcPath"));
                artifact.setTargetDirPath(targetFileName.get("targetPath"));
            }
        }
    }

    @Override
    public void setFlatDownload(boolean flat) {
        this.downloader.setFlatDownload(flat);
    }

    public String buildAqlSearchQuery(String searchPattern, String[] excludePatterns, boolean recursive, String props) {
        searchPattern = prepareSearchPattern(searchPattern, true);
        int repoIndex = searchPattern.indexOf("/");
        String repo = searchPattern.substring(0, repoIndex);
        searchPattern = searchPattern.substring(repoIndex + 1);

        List<PathFilePair> pairs = createPathFilePairs(searchPattern, recursive);
        List<PathFilePair> excludePairs = Lists.newArrayList();
        if (excludePatterns != null) {
            for (String excludePattern : excludePatterns) {
                excludePairs.addAll(createPathFilePairs(prepareSearchPattern(excludePattern, false), recursive));
            }
        }
        int size = pairs.size();

        String json = "{" + "\"repo\": \"" + repo + "\"," + buildPropsQuery(props) + "\"$or\": [";

        if (size == 0) {
            json += "{" + buildInnerQuery(".", searchPattern, true, excludePairs, recursive) + "}";
        } else {
            for (int i = 0; i < size; i++) {
                json += "{" + buildInnerQuery(pairs.get(i).getPath(), pairs.get(i).getFile(), !searchPattern.contains("/"), excludePairs, recursive) + "}";

                if (i + 1 < size) {
                    json += ",";
                }
            }
        }

        return json + "]}";
    }

    private String prepareSearchPattern(String pattern, boolean startsWithRepo) {
        if (startsWithRepo && !pattern.contains("/")) {
            pattern += "/";
        }
        if (pattern.endsWith("/")) {
            pattern += "*";
        }
        return pattern.replaceAll("[()]", "");
    }

    private String buildPropsQuery(String props) {
        if (props.equals("")) {
            return "";
        }
        String[] propList = props.split(";");
        String query = "";
        for (int i = 0; i < propList.length; i++) {
            String[] keyVal = propList[i].split("=");
            if (keyVal.length != 2) {
                System.out.print("Invalid props pattern: " + propList[i]);
            }
            String key = keyVal[0];
            String value = keyVal[1];
            query += "\"@" + key + "\": {\"$match\" : \"" + value + "\"},";
        }
        return query;
    }

    private String buildInnerQuery(String path, String name, boolean includeRoot, List<PathFilePair> excludePairs, boolean recursive) {
        StringBuilder excludePattern = new StringBuilder();

        String nePath = "";
        if (!includeRoot) {
            nePath = ", {\"path\": {\"$ne\": \".\"}}";
        }

        if (excludePairs != null) {
            for (PathFilePair singleExcludePattern : excludePairs) {
                String excludePath = singleExcludePattern.getPath();
                if (!recursive && ".".equals(excludePath)) {
                    excludePath = path;
                }
                excludePattern.append(String.format(", {\"$or\": [{\"path\": {\"$nmatch\": \"%s\"}}, {\"name\": {\"$nmatch\": \"%s\"}}]}", excludePath, singleExcludePattern.getFile()));
            }
        }
        return String.format(
                "\"$and\": [{" +
                    "\"$and\": [" +
                        "{\"path\": { \"$match\":" + "\"%s\"}}%s%s]," +
                    "\"$and\": [" +
                        "{\"name\": { \"$match\":" + "\"%s\"}}]" +
                "}]", path, nePath, excludePattern, name);
    }

    // We need to translate the provided download pattern to an AQL query.
    // In Artifactory, for each artifact the name and path of the artifact are saved separately.
    // We therefore need to build an AQL query that covers all possible paths and names the provided
    // pattern can include.
    // For example, the pattern a/* can include the two following files:
    // a/file1.tgz and also a/b/file2.tgz
    // To achieve that, this function parses the pattern by splitting it by its * characters.
    // The end result is a list of PathFilePair structs.
    // Each struct represent a possible path and file name pair to be included in AQL query with an "or" relationship.
    private List<PathFilePair> createPathFilePairs(String pattern, boolean recursive) {
        String defaultPath;
        if (recursive) {
            defaultPath = "*";
        } else {
            defaultPath = ".";
        }

        List<PathFilePair> pairs = new ArrayList<PathFilePair>();
        if (pattern.equals("*")) {
            pairs.add(new PathFilePair(defaultPath, "*"));
            return pairs;
        }

        int slashIndex = pattern.lastIndexOf("/");
        String path;
        String name;
        if (slashIndex < 0) {
            pairs.add(new PathFilePair(".", pattern));
            path = "";
            name = pattern;
        } else {
            path = pattern.substring(0, slashIndex);
            name = pattern.substring(slashIndex + 1);
            pairs.add(new PathFilePair(path, name));
        }
        if (!recursive) {
            return pairs;
        }
        if (name.equals("*")) {
            path += "/*";
            pairs.add(new PathFilePair(path, "*"));
            return pairs;
        }
        pattern = name;

        String[] sections = pattern.split("\\*", -1);
        int size = sections.length;
        for (int i = 0; i < size; i++) {
            List<String> options = new ArrayList<String>();
            if (i + 1 < size) {
                options.add(sections[i] + "*/");
            }
            for (String option : options) {
                String str = "";
                for (int j = 0; j < size; j++) {
                    if (j > 0) {
                        str += "*";
                    }
                    if (j == i) {
                        str += option;
                    } else {
                        str += sections[j];
                    }
                }
                String[] split = str.split("/", -1);
                String filePath = split[0];
                String fileName = split[1];
                if (fileName.equals("")) {
                    fileName = "*";
                }
                if (!path.equals("") && !path.endsWith("/")) {
                    path += "/";
                }
                pairs.add(new PathFilePair(path + filePath, fileName));
            }
        }
        return pairs;
    }

    private class PathFilePair {
        private String path;
        private String file;

        public PathFilePair(String path, String file) {
            this.path = path;
            this.file = file;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }
    }
}
