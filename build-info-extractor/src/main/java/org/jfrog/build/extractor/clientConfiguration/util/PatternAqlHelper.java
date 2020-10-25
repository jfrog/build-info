package org.jfrog.build.extractor.clientConfiguration.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.spec.FileSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PatternAqlHelper extends AqlHelperBase {
    PatternAqlHelper(ArtifactoryDependenciesClient client, Log log, FileSpec file) throws IOException {
        super(client, log, file);
    }

    @Override
    public void convertFileSpecToAql(FileSpec file) throws IOException {
        super.buildQueryAdditionalParts(file);
        boolean recursive = !"false".equalsIgnoreCase(file.getRecursive());
        this.queryBody = buildAqlSearchQuery(file.getPattern(), file.getExcludePatterns(), file.getExclusions(), recursive, file.getProps());
    }

    static String buildAqlSearchQuery(String searchPattern, String[] excludePatterns, String[] exclusions, boolean recursive, String props) {
        // Prepare.
        searchPattern = prepareSearchPattern(searchPattern);

        // Create triples.
        List<RepoPathFile> repoPathFileTriples = createRepoPathFileTriples(searchPattern, recursive);
        boolean includeRoot = StringUtils.countMatches(searchPattern, "/") < 2;
        int triplesSize = repoPathFileTriples.size();

        // Build  query.
        String excludeQuery = buildExcludeQuery(excludePatterns, exclusions,triplesSize == 0 || recursive, recursive);
        String nePath = buildNePathQuery(triplesSize == 0 || includeRoot);
        String json = String.format("{%s\"$or\":[", buildPropsQuery(props) + nePath + excludeQuery);
        StringBuilder aqlQuery = new StringBuilder(json);
        aqlQuery.append(handleRepoPathFileTriples(repoPathFileTriples, triplesSize)).append("]}");

        return aqlQuery.toString();
    }

    static String handleRepoPathFileTriples(List<RepoPathFile> repoPathFiles, int repoPathFileSize) {
        String query = "";
        for (int i = 0; i < repoPathFileSize; i++) {
            query += buildInnerQuery(repoPathFiles.get(i));

            if (i + 1 < repoPathFileSize) {
                query += ",";
            }
        }
        return query;
    }

    private static String prepareSearchPattern(String pattern) {
        if (pattern.endsWith("/")) {
            pattern += "*";
        }
        return pattern.replaceAll("[()]", "");
    }

    private static boolean isSlashPrecedeAsterisk(int asteriskIndex, int slashIndex) {
        return slashIndex < asteriskIndex && slashIndex >= 0;
    }

    static List<RepoPathFile> createRepoPathFileTriples(String searchPattern, boolean recursive) {
        int firstSlashIndex = searchPattern.indexOf("/");
        List<Integer> asteriskIndices = new ArrayList<>();
        for (int i = 0; i < searchPattern.length(); i++) {
            if (searchPattern.charAt(i) == '*') {
                asteriskIndices.add(i);
            }
        }

        if (!asteriskIndices.isEmpty() && !isSlashPrecedeAsterisk(asteriskIndices.get(0), firstSlashIndex)) {
            List<RepoPathFile> triples = new ArrayList<>();
            int lastRepoAsteriskIndex = 0;
            for (int asteriskIndex : asteriskIndices) {
                if (isSlashPrecedeAsterisk(asteriskIndex, firstSlashIndex)) {
                    break;
                }
                String repo = searchPattern.substring(0, asteriskIndex + 1); // '<repo>*'
                String newPattern = searchPattern.substring(asteriskIndex);  // '*<pattern>'

                // If slashCount or asterixCount are 1 or less, don't trim prefix of '*/' to allow specific-name enforce in triple.
                // For example, in case of pattern '*/a1.in', the calculated triple should contain 'a1.in' as the 'file'.
                int slashCount = StringUtils.countMatches(newPattern, "/");
                int asterixCount = StringUtils.countMatches(newPattern, "*");
                if (slashCount > 1 || asterixCount > 1) {
                    // Remove '/' character as the pattern precedes it may be the repository name.
                    // Leaving the '/' forces another hierarchy in the 'path' of the triple, which isn't correct.
                    newPattern = newPattern.replaceFirst("^\\*/", "");
                    if (!newPattern.startsWith("*")) {
                        newPattern = "*" + newPattern;
                    }
                }

                triples.addAll(createPathFilePairs(repo, newPattern, recursive));
                lastRepoAsteriskIndex = asteriskIndex + 1;
            }

            // Handle characters between last asterisk before first slash: "a*handle-it/".
            if (lastRepoAsteriskIndex < firstSlashIndex) {
                String repo = searchPattern.substring(0, firstSlashIndex);        // '<repo>*'
                String newPattern = searchPattern.substring(firstSlashIndex + 1); // '*<pattern>'
                triples.addAll(createPathFilePairs(repo, newPattern, recursive));
            } else if (firstSlashIndex < 0 && !StringUtils.endsWith(searchPattern, "*")) {
                // Handle characters after last asterisk "a*handle-it".
                triples.addAll(createPathFilePairs(searchPattern, "*", recursive));
            }

            return triples;
        }

        if (firstSlashIndex < 0) {
            return createPathFilePairs(searchPattern, "*", recursive);
        }
        String repo = searchPattern.substring(0, firstSlashIndex);
        String pattern = searchPattern.substring(firstSlashIndex + 1);
        return createPathFilePairs(repo, pattern, recursive);
    }


    // We need to translate the provided pattern to an AQL query.
    // In Artifactory, for each artifact the name and path of the artifact are saved separately.
    // We therefore need to build an AQL query that covers all possible repositories, paths and names the provided
    // pattern can include.
    // For example, the pattern repo/a/* can include the two following files:
    // repo/a/file1.tgz and also repo/a/b/file2.tgz
    // To achieve that, this function parses the pattern by splitting it by its * characters.
    // The end result is a list of RepoPathFilePair objects.
    // Each object represent a possible repository, path and file name triple to be included in AQL query with an "or" relationship.
    static List<RepoPathFile> createPathFilePairs(String repo, String pattern, boolean recursive) {
        List<RepoPathFile> res = new ArrayList<>();
        if (pattern.equals("*")) {
            res.add(new RepoPathFile(repo, getDefaultPath(recursive), "*"));
            return res;
        }

        String path;
        String name;
        List<RepoPathFile> triples = new ArrayList<>();

        // Handle non-recursive triples.
        int slashIndex = pattern.lastIndexOf("/");
        if (slashIndex < 0) {
            // Optimization - If pattern starts with '*', we'll have a triple with <repo>*<file>.
            // In that case we'd prefer to avoid <repo>.<file>.
            if (recursive && pattern.startsWith("*")) {
                path = "";
                name = pattern;
            } else {
                path = "";
                name = pattern;
                triples.add(new RepoPathFile(repo, ".", pattern));
            }
        } else {
            path = pattern.substring(0, slashIndex);
            name = pattern.substring(slashIndex + 1);
            triples.add(new RepoPathFile(repo, path, name));
        }

        if (!recursive) {
            return triples;
        }
        if (name.equals("*")) {
            triples.add(new RepoPathFile(repo, path + "/*", "*"));
            return triples;
        }

        String[] nameSplit = name.split("\\*", -1);
        for (int i = 0; i < nameSplit.length - 1; i++) {
            String str = "";
            for (int j = 0; j < nameSplit.length; j++) {
                String namePart = nameSplit[j];
                if (j > 0) {
                    str += "*";
                }
                if (j == i) {
                    str += nameSplit[i] + "*/";
                } else {
                    str += namePart;
                }
            }
            String[] slashSplit = str.split("/", -1);
            String filePath = slashSplit[0];
            String fileName = slashSplit[1];
            if (fileName.equals("")) {
                fileName = "*";
            }
            if (!path.equals("") && !path.endsWith("/")) {
                path += "/";
            }
            triples.add(new RepoPathFile(repo, path + filePath, fileName));
        }

        return triples;
    }

    private static String getDefaultPath(boolean recursive) {
        if (recursive) {
            return "*";
        }
        return ".";
    }

    private static String buildExcludeQuery(String[] excludePatterns, String[] exclusions, boolean useLocalPath, boolean recursive) {
        if (ArrayUtils.isEmpty(exclusions) && ArrayUtils.isEmpty(excludePatterns)) {
            return "";
        }
        List<RepoPathFile> excludeTriples = new ArrayList<>();
        if (exclusions != null && exclusions.length > 0) {
            for (String exclusion : exclusions) {
                excludeTriples.addAll(createRepoPathFileTriples(prepareSearchPattern(exclusion), recursive));
            }
        } else {
            // Support legacy exclude patterns. 'Exclude patterns' are deprecated and replaced by 'exclusions'.
            for (String excludePattern : excludePatterns) {
                excludeTriples.addAll(createPathFilePairs("", prepareSearchPattern(excludePattern), recursive));
            }
        }

        String excludeQuery = "";
        for (RepoPathFile excludeTriple : excludeTriples) {
            String excludePath = excludeTriple.getPath();
            if (!useLocalPath && excludePath.equals(".")) {
                excludePath = "*";
            }
            String excludeRepoStr = "";
            if (StringUtils.isNotEmpty(excludeTriple.getRepo())) {
                excludeRepoStr = String.format("\"repo\":{\"$nmatch\":\"%s\"},", excludeTriple.getRepo());
            }
            excludeQuery += String.format("\"$or\":[{%s\"path\":{\"$nmatch\":\"%s\"},\"name\":{\"$nmatch\":\"%s\"}}],",
                    excludeRepoStr, excludePath, excludeTriple.getFile());
        }
        return excludeQuery;
    }

    private static String buildPropsQuery(String props) {
        if (props == null || props.equals("")) {
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

    private static String buildInnerQuery(RepoPathFile triple) {
        return String.format(
                "{\"$and\":[{" +
                        "\"repo\":%s," +
                        "\"path\":%s," +
                        "\"name\":%s" +
                        "}]}",
                getAqlValue(triple.getRepo()), getAqlValue(triple.getPath()), getAqlValue(triple.getFile()));
    }

    // Optimization - If value is wildcard pattern, return '{"$match":"value"}'.
    // Otherwise, return '"value"'.
    private static String getAqlValue(String value) {
        String aqlValuePattern;
        if (value.contains("*")) {
            aqlValuePattern = "{\"$match\":\"%s\"}";
        } else {
            aqlValuePattern = "\"%s\"";
        }
        return String.format(aqlValuePattern, value);
    }

    private static String buildNePathQuery(boolean includeRoot) {
        return includeRoot ? "" : "\"path\":{\"$ne\":\".\"},";
    }

    static class RepoPathFile {
        private String repo;
        private String path;
        private String file;

        RepoPathFile(String repo, String path, String file) {
            this.repo = repo;
            this.path = path;
            this.file = file;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RepoPathFile)) return false;
            RepoPathFile that = (RepoPathFile) o;
            return Objects.equals(repo, that.repo) &&
                    Objects.equals(path, that.path) &&
                    Objects.equals(file, that.file);
        }

        @Override
        public String toString() {
            return "RepoPathFile{" +
                    "repo='" + repo + '\'' +
                    ", path='" + path + '\'' +
                    ", file='" + file + '\'' +
                    '}';
        }
    }
}
