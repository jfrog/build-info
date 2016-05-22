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
public class WildcardDependenciesHelper implements DependenciesHelper {
    private DependenciesDownloader downloader;
    private Log log;
    private String artifactoryUrl;
    private String target;
    private String props;
    private boolean recursive;

    public WildcardDependenciesHelper(DependenciesDownloader downloader, String artifactoryUrl, String target, Log log) {
        this.downloader = downloader;
        this.log = log;
        this.artifactoryUrl = artifactoryUrl;
        this.target = target;

        this.recursive = false;
        this.props = "";
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
        this.props = props;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }

    @Override
    public List<Dependency> retrievePublishedDependencies(String searchPattern)
            throws IOException, InterruptedException {
        if (StringUtils.isBlank(searchPattern)) {
            return Collections.emptyList();
        }
        AqlDependenciesHelper dependenciesHelper = new AqlDependenciesHelper(downloader, artifactoryUrl, target, log);
        Set<DownloadableArtifact> downloadableArtifacts = dependenciesHelper.collectArtifactsToDownload(buildAqlSearchQuery(searchPattern, this.recursive, this.props));
        replaceTargetPlaceholders(searchPattern, downloadableArtifacts);
        return dependenciesHelper.downloadDependencies(downloadableArtifacts);
    }

    private void replaceTargetPlaceholders(String searchPattern, Set<DownloadableArtifact> downloadableArtifacts) {
        Pattern pattern = Pattern.compile(PlaceholderReplacementUtils.pathToRegExp(searchPattern));
        for (DownloadableArtifact artifact : downloadableArtifacts) {
            String repoName = StringUtils.substringAfterLast(artifact.getRepoUrl(), "/");
            if (target.endsWith("/")) {
                artifact.setTargetDirPath(PlaceholderReplacementUtils.reformatRegexp(repoName + "/" + artifact.getFilePath(),
                        target, pattern));
            } else {
                String targetAfterReplacement = PlaceholderReplacementUtils.reformatRegexp(repoName + "/" + artifact.getFilePath(),
                        target, pattern);
                Map<String, String> targetFileName = PlaceholderReplacementUtils.replaceFilesName(targetAfterReplacement, artifact.getRelativeDirPath());
                artifact.setRelativeDirPath(targetFileName.get("srcPath"));
                artifact.setTargetDirPath(targetFileName.get("targetPath"));
            }
        }
    }

    @Override
    public void setFlatDownload(boolean flat) {
        this.downloader.setFlatDownload(flat);
    }

    public String buildAqlSearchQuery(String searchPattern, boolean recursive, String props) {
        searchPattern = prepareSearchPattern(searchPattern);
        int index = searchPattern.indexOf("/");

        String repo = searchPattern.substring(0, index);
        searchPattern = searchPattern.substring(index + 1);

        List<PathFilePair> pairs = createPathFilePairs(searchPattern, recursive);
        int size = pairs.size();

        String json =
                "{" +
                        "\"repo\": \"" + repo + "\"," +
                        buildPropsQuery(props) +
                        "\"$or\": [";

        if (size == 0) {
            json +=
                    "{" +
                            buildInnerQuery(".", searchPattern) +
                            "}";
        } else {
            for (int i = 0; i < size; i++) {
                json +=
                        "{" +
                                buildInnerQuery(pairs.get(i).getPath(), pairs.get(i).getFile()) +
                                "}";

                if (i + 1 < size) {
                    json += ",";
                }
            }
        }

        json +=
                "]" +
                        "}";

        return "items.find(" + json + ")";
    }

    private String prepareSearchPattern(String pattern) {
        int index = pattern.indexOf("/");
        if (index < 0) {
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

    private String buildInnerQuery(String path, String name) {
        return "\"$and\": [{" +
                "\"path\": {" +
                "\"$match\":" + "\"" + path + "\"" +
                "}," +
                "\"name\":{" +
                "\"$match\":" + "\"" + name + "\"" +
                "}" +
                "}]";
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
        String path = "";
        String name = "";
        if (slashIndex < 0) {
            pairs.add(new PathFilePair(".", pattern));
            path = "";
            name = pattern;
        } else {
            if (slashIndex >= 0) {
                path = pattern.substring(0, slashIndex);
                name = pattern.substring(slashIndex + 1);
                pairs.add(new PathFilePair(path, name));
            }
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

        String[] sections = pattern.split("\\*");
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
                String[] split = str.split("/");
                String filePath = split[0];
                String fileName = split[1];
                if (fileName.equals("")) {
                    fileName = "*";
                }
                if (!path.equals("")) {
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
