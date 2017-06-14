package org.jfrog.build.extractor.clientConfiguration.util;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.dependency.DownloadableArtifact;
import org.jfrog.build.api.dependency.pattern.PatternType;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Created by Tamirh on 25/04/2016.
 */
public class AqlDependenciesHelper implements DependenciesHelper {

    private DependenciesDownloader downloader;
    private Log log;
    private String artifactoryUrl;
    private String target;
    private String buildName;
    private String buildNumber;

    public AqlDependenciesHelper(DependenciesDownloader downloader, String target, Log log) {
        this.downloader = downloader;
        this.log = log;
        this.artifactoryUrl = downloader.getClient().getArtifactoryUrl();
        this.target = target;
    }

    @Override
    public List<Dependency> retrievePublishedDependencies(String aql)
            throws IOException {
        if (StringUtils.isBlank(aql)) {
            return Collections.emptyList();
        }
        Set<DownloadableArtifact> downloadableArtifacts = collectArtifactsToDownload(aql);
        return downloadDependencies(downloadableArtifacts);
    }

    public List<Dependency> downloadDependencies(Set<DownloadableArtifact> downloadableArtifacts) throws IOException {
        log.info("Beginning to resolve Build Info published dependencies.");
        List<Dependency> dependencies = downloader.download(downloadableArtifacts);
        log.info("Finished resolving Build Info published dependencies.");
        return dependencies;
    }

    @Override
    public void setFlatDownload(boolean flat) {
        this.downloader.setFlatDownload(flat);
    }

    public Set<DownloadableArtifact> collectArtifactsToDownload(String aql) throws IOException {
        Set<DownloadableArtifact> downloadableArtifacts = Sets.newHashSet();
        if (StringUtils.isNotBlank(buildName)) {
            aql = addBuildToQuery(aql);
        }
        aql = "items.find(" + aql + ")";
        AqlSearchResult aqlSearchResult = downloader.getClient().searchArtifactsByAql(aql);
        List<AqlSearchResult.SearchEntry> searchResults = aqlSearchResult.getResults();
        for (AqlSearchResult.SearchEntry searchEntry : searchResults) {
            String path = searchEntry.getPath().equals(".") ? "" : searchEntry.getPath() + "/";
            downloadableArtifacts.add(new DownloadableArtifact(StringUtils.stripEnd(artifactoryUrl, "/") + "/" +
                    searchEntry.getRepo(), target, path + searchEntry.getName(), "", "", PatternType.NORMAL));
        }
        return downloadableArtifacts;
    }

    private String addBuildToQuery(String aql) {
        return "{" +
                "\"$and\": [" +
                    aql + "," +
                    "{" +
                        "\"artifact.module.build.name\": {" +
                            "\"$eq\": \"" + buildName + "\"" +
                        "}" +
                "}," +
                "{" +
                        "\"artifact.module.build.number\": {" +
                            "\"$eq\": \"" + buildNumber + "\"" +
                        "}" +
                    "}" +
                    "]" +
                "}";
    }

    public void setArtifactoryUrl(String artifactoryUrl) {
        this.artifactoryUrl = artifactoryUrl;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getArtifactoryUrl() {
        return artifactoryUrl;
    }

    public String getBuildName() {
        return buildName;
    }

    public void setBuildName(String buildName) {
        this.buildName = buildName;
    }

    public String getBuildNumber() {
        return buildNumber;
    }

    public void setBuildNumber(String buildNumber) {
        this.buildNumber = buildNumber;
    }
}