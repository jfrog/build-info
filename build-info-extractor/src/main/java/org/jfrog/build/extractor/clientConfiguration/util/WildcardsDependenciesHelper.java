package org.jfrog.build.extractor.clientConfiguration.util;

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
        searchPattern = StringUtils.substringAfter(searchPattern, "/");
        Pattern pattern = Pattern.compile(PathsUtils.pathToRegExp(searchPattern));
        target = StringUtils.defaultIfEmpty(target , "");
        for (DownloadableArtifact artifact : downloadableArtifacts) {
            if (StringUtils.isEmpty(target) || target.endsWith("/")) {
                artifact.setTargetDirPath(PathsUtils.reformatRegexp(artifact.getFilePath(), target, pattern));
            } else {
                String targetAfterReplacement = PathsUtils.reformatRegexp(artifact.getFilePath(), target, pattern);
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
        StringBuilder aqlQuery = new StringBuilder();
        searchPattern = prepareSearchPattern(searchPattern, true);
        int repoIndex = searchPattern.indexOf("/");
        String repo = searchPattern.substring(0, repoIndex);
        searchPattern = searchPattern.substring(repoIndex + 1);

        List<PathFilePair> pathFilePairs = createPathFilePairs(searchPattern, recursive);
        int pathFilePairsSize = pathFilePairs.size();

        String excludeQuery = buildExcludeQuery(excludePatterns, pathFilePairsSize == 0 || recursive);
        String nePath = buildNePathQuery(pathFilePairsSize == 0 || !searchPattern.contains("/"));
        aqlQuery.append("{\"repo\": \"").append(repo).append("\",").append(buildPropsQuery(props)).append(nePath).append(excludeQuery).append("\"$or\": [");
        if (pathFilePairsSize == 0) {
            aqlQuery.append(buildInnerQuery(".", searchPattern));
        } else {
            for (PathFilePair pair : pathFilePairs) {
                aqlQuery.append(buildInnerQuery(pair.getPath(), pair.getFile())).append(",");
            }
            aqlQuery.deleteCharAt(aqlQuery.length() - 1);
        }
        return aqlQuery.append("]}").toString();
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
        StringBuilder query = new StringBuilder();
        for (String prop : propList) {
            String[] keyVal = prop.split("=");
            if (keyVal.length != 2) {
                System.out.print("Invalid props pattern: " + prop);
            }
            String key = keyVal[0];
            String value = keyVal[1];
            query.append("\"@").append(key).append("\": {\"$match\" : \"").append(value).append("\"},");
        }
        return query.toString();
    }

    private String buildExcludeQuery(String[] excludePatterns, boolean useLocalPath) {
        if (excludePatterns == null) {
            return "";
        }
        List<PathFilePair> excludePairs = new ArrayList<>();
        for (String excludePattern : excludePatterns) {
            excludePairs.addAll(createPathFilePairs(prepareSearchPattern(excludePattern, false), recursive));
        }
        StringBuilder excludeQuery = new StringBuilder();
        for (PathFilePair singleExcludePattern : excludePairs) {
            String excludePath = singleExcludePattern.getPath();
            if (!useLocalPath && ".".equals(excludePath)) {
                excludePath = "*";
            }
            excludeQuery.append(String.format("\"$or\": [{\"path\": {\"$nmatch\": \"%s\"}, \"name\": {\"$nmatch\": \"%s\"}}],", excludePath, singleExcludePattern.getFile()));
        }
        return excludeQuery.toString();
    }

    private String buildNePathQuery(boolean includeRoot) {
        return includeRoot ? "" : "\"path\": {\"$ne\": \".\"}, ";
    }

    private String buildInnerQuery(String path, String name) {
         return String.format(
                "{\"$and\": [{" +
                    "\"path\": { \"$match\": \"%s\"}," +
                    "\"name\": { \"$match\": \"%s\"}" +
                "}]}", path, name);
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
