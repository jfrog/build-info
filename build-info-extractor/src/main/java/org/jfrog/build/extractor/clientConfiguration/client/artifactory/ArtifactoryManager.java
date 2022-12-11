package org.jfrog.build.extractor.clientConfiguration.client.artifactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.jfrog.build.api.dependency.BuildPatternArtifacts;
import org.jfrog.build.api.dependency.BuildPatternArtifactsRequest;
import org.jfrog.build.api.dependency.PatternResultFileSet;
import org.jfrog.build.api.dependency.PropertySearchResult;
import org.jfrog.build.api.release.Distribution;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.client.DownloadResponse;
import org.jfrog.build.client.ItemLastModified;
import org.jfrog.build.client.artifactoryXrayResponse.ArtifactoryXrayResponse;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.BuildRetention;
import org.jfrog.build.extractor.clientConfiguration.client.ManagerBase;
import org.jfrog.build.extractor.clientConfiguration.client.RepositoryType;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.*;
import org.jfrog.build.extractor.clientConfiguration.client.response.GetAllBuildNumbersResponse;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.usageReport.UsageReporter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.extractor.clientConfiguration.client.artifactory.services.ScanBuild.XRAY_SCAN_CONNECTION_TIMEOUT_SECS;

public class ArtifactoryManager extends ManagerBase {
    public static final String LATEST = "LATEST";
    public static final String LAST_RELEASE = "LAST_RELEASE";

    public ArtifactoryManager(String artifactoryUrl, String username, String password, String accessToken, Log log) {
        super(artifactoryUrl, username, password, accessToken, log);
    }

    public ArtifactoryManager(String artifactoryUrl, String username, String password, Log log) {
        super(artifactoryUrl, username, password, StringUtils.EMPTY, log);
    }

    public ArtifactoryManager(String artifactoryUrl, String accessToken, Log log) {
        super(artifactoryUrl, StringUtils.EMPTY, StringUtils.EMPTY, accessToken, log);
    }

    public ArtifactoryManager(String artifactoryUrl, Log log) {
        super(artifactoryUrl, StringUtils.EMPTY, StringUtils.EMPTY, StringUtils.EMPTY, log);
    }

    public boolean isLocalRepo(String repositoryKey) throws IOException {
        CheckRepositoryType checkRepositoryTypeService = new CheckRepositoryType(RepositoryType.LOCAL, repositoryKey, log);
        return checkRepositoryTypeService.execute(jfrogHttpClient);
    }

    public boolean isRemoteRepo(String repositoryKey) throws IOException {
        CheckRepositoryType checkRepositoryTypeService = new CheckRepositoryType(RepositoryType.REMOTE, repositoryKey, log);
        return checkRepositoryTypeService.execute(jfrogHttpClient);
    }

    public void deleteProperties(String relativePath, String properties) throws IOException {
        DeleteProperties deletePropertiesService = new DeleteProperties(relativePath, properties, log);
        deletePropertiesService.execute(jfrogHttpClient);
    }

    public void setProperties(String relativePath, String properties, boolean encodeProperties) throws IOException {
        SetProperties setPropertiesService = new SetProperties(relativePath, properties, encodeProperties, log);
        setPropertiesService.execute(jfrogHttpClient);
    }

    public void setProperties(String relativePath, ArrayListMultimap<String, String> properties, boolean encodeProperties) throws IOException {
        SetProperties setPropertiesService = new SetProperties(relativePath, properties, encodeProperties, log);
        setPropertiesService.execute(jfrogHttpClient);
    }

    public void distributeBuild(String buildName, String buildNumber, Distribution promotion) throws IOException {
        DistributeBuild distributeBuildService = new DistributeBuild(buildName, buildNumber, promotion, log);
        distributeBuildService.execute(jfrogHttpClient);
    }

    public DownloadResponse download(String downloadFrom) throws IOException {
        return download(downloadFrom, null);
    }

    public DownloadResponse download(String downloadFrom, Map<String, String> headers) throws IOException {
        Download downloadService = new Download(downloadFrom, headers, log);
        return downloadService.execute(jfrogHttpClient);
    }

    public Header[] downloadHeaders(String downloadFrom) throws IOException {
        return downloadHeaders(downloadFrom, null);
    }

    public Header[] downloadHeaders(String downloadFrom, Map<String, String> headers) throws IOException {
        DownloadHeaders downloadHeadersService = new DownloadHeaders(downloadFrom, headers, log);
        return downloadHeadersService.execute(jfrogHttpClient);
    }

    public File downloadToFile(String downloadFrom, String downloadTo) throws IOException {
        return downloadToFile(downloadFrom, downloadTo, null);
    }

    public File downloadToFile(String downloadFrom, String downloadTo, Map<String, String> headers) throws IOException {
        DownloadToFile downloadToFileService = new DownloadToFile(downloadFrom, downloadTo, headers, log);
        return downloadToFileService.execute(jfrogHttpClient);
    }

    public void executeUserPlugin(String executionName, Map<String, String> requestParams) throws IOException {
        ExecuteUserPlugin executeUserPluginService = new ExecuteUserPlugin(executionName, requestParams, log);
        executeUserPluginService.execute(jfrogHttpClient);
    }

    public void promotionUserPlugin(String promotionName, String buildName, String buildNumber, Map<String, String> requestParams) throws IOException {
        PromotionUserPlugin promotionUserPlugin = new PromotionUserPlugin(promotionName, buildName, buildNumber, requestParams, log);
        promotionUserPlugin.execute(jfrogHttpClient);
    }

    public ItemLastModified getItemLastModified(String path) throws IOException {
        GetItemLastModified getItemLastModifiedService = new GetItemLastModified(path, log);
        return getItemLastModifiedService.execute(jfrogHttpClient);
    }

    public Properties getNpmAuth() throws IOException {
        GetNpmAuth getNpmAuthService = new GetNpmAuth(log);
        return getNpmAuthService.execute(jfrogHttpClient);
    }

    public ArtifactoryVersion getVersion() throws IOException {
        Version versionService = new Version(log);
        return versionService.execute(jfrogHttpClient);
    }

    public void publishBuildInfo(BuildInfo buildInfo, String platformUrl) throws IOException {
        PublishBuildInfo publishBuildInfoService = new PublishBuildInfo(buildInfo, platformUrl, log);
        publishBuildInfoService.execute(jfrogHttpClient);
    }

    public void sendModuleInfo(BuildInfo buildInfo) throws IOException {
        SendModuleInfo sendModuleInfoService = new SendModuleInfo(buildInfo, log);
        sendModuleInfoService.execute(jfrogHttpClient);
    }

    public GetAllBuildNumbersResponse getAllBuildNumbers(String buildName, String project) throws IOException {
        GetAllBuildNumbers getAllBuildNumbersService = new GetAllBuildNumbers(buildName, project, log);
        return getAllBuildNumbersService.execute(jfrogHttpClient);
    }

    public void deleteBuilds(String buildName, String project, boolean deleteArtifacts) throws IOException {
        DeleteBuilds deleteBuildsService = new DeleteBuilds(buildName, project, deleteArtifacts, log);
        deleteBuildsService.execute(jfrogHttpClient);
    }

    public void deleteBuilds(String buildName, String project, boolean deleteArtifacts, String... buildNumbers) throws IOException {
        DeleteBuilds deleteBuildsService = new DeleteBuilds(buildName, project, buildNumbers, deleteArtifacts, log);
        deleteBuildsService.execute(jfrogHttpClient);
    }

    public BuildInfo getBuildInfo(String buildName, String buildNumber, String project) throws IOException {
        if (LATEST.equals(buildNumber.trim()) || LAST_RELEASE.equals(buildNumber.trim())) {
            buildNumber = getLatestBuildNumber(buildName, buildNumber, project);
            if (buildNumber == null) {
                return null;
            }
        }
        GetBuildInfo getBuildInfoService = new GetBuildInfo(buildName, buildNumber, project, log);
        return getBuildInfoService.execute(jfrogHttpClient);
    }


    public List<String> getLocalRepositoriesKeys() throws IOException {
        GetRepositoriesKeys getLocalRepositoriesKeysService = new GetRepositoriesKeys(RepositoryType.LOCAL, log);
        return getLocalRepositoriesKeysService.execute(jfrogHttpClient);
    }

    public List<String> getRemoteRepositoriesKeys() throws IOException {
        GetRepositoriesKeys getRemoteRepositoriesKeysService = new GetRepositoriesKeys(RepositoryType.REMOTE, log);
        return getRemoteRepositoriesKeysService.execute(jfrogHttpClient);
    }

    public List<String> getVirtualRepositoriesKeys() throws IOException {
        GetRepositoriesKeys getVirtualRepositoriesKeysService = new GetRepositoriesKeys(RepositoryType.VIRTUAL, log);
        return getVirtualRepositoriesKeysService.execute(jfrogHttpClient);
    }

    public List<String> getFederatedRepositoriesKeys() throws IOException {
        GetRepositoriesKeys getVirtualRepositoriesKeysService = new GetRepositoriesKeys(RepositoryType.FEDERATED, log);
        return getVirtualRepositoriesKeysService.execute(jfrogHttpClient);
    }

    public Map getStagingStrategy(String strategyName, String buildName, Map<String, String> requestParams) throws IOException {
        GetStagingStrategy getStagingStrategyService = new GetStagingStrategy(strategyName, buildName, requestParams, log);
        return getStagingStrategyService.execute(jfrogHttpClient);
    }

    public Map<String, List<Map>> getUserPluginInfo() throws IOException {
        GetUserPluginInfo getUserPluginInfoService = new GetUserPluginInfo(log);
        return getUserPluginInfoService.execute(jfrogHttpClient);
    }

    public boolean isRepositoryExist(String repositoryKey) throws IOException {
        IsRepositoryExist IsRepositoryExistService = new IsRepositoryExist(repositoryKey, log);
        return IsRepositoryExistService.execute(jfrogHttpClient);
    }

    public void reportUsage(UsageReporter usageReporter) throws IOException {
        ReportUsage reportUsageService = new ReportUsage(usageReporter, log);
        reportUsageService.execute(jfrogHttpClient);
    }

    public List<BuildPatternArtifacts> retrievePatternArtifacts(List<BuildPatternArtifactsRequest> requests) throws IOException {
        RetrievePatternArtifacts retrievePatternArtifactsService = new RetrievePatternArtifacts(requests, log);
        return retrievePatternArtifactsService.execute(jfrogHttpClient);
    }

    public ArtifactoryXrayResponse scanBuild(String buildName, String buildNumber, String project, String context) throws IOException {
        setConnectionTimeout(XRAY_SCAN_CONNECTION_TIMEOUT_SECS);
        ScanBuild scanBuildService = new ScanBuild(buildName, buildNumber, project, context, log);
        return scanBuildService.execute(jfrogHttpClient);
    }

    public AqlSearchResult searchArtifactsByAql(String aql) throws IOException {
        SearchArtifactsByAql searchArtifactsByAqlService = new SearchArtifactsByAql(aql, log);
        return searchArtifactsByAqlService.execute(jfrogHttpClient);
    }

    public PatternResultFileSet searchArtifactsByPattern(String pattern) throws IOException {
        SearchArtifactsByPattern searchArtifactsByPatternService = new SearchArtifactsByPattern(pattern, log);
        return searchArtifactsByPatternService.execute(jfrogHttpClient);
    }

    public PropertySearchResult searchArtifactsByProperties(String properties) throws IOException {
        SearchArtifactsByProperties searchArtifactsByPropertiesService = new SearchArtifactsByProperties(properties, log);
        return searchArtifactsByPropertiesService.execute(jfrogHttpClient);
    }

    public void sendBuildRetention(BuildRetention buildRetention, String buildName, String project, boolean async) throws IOException {
        SendBuildRetention sendBuildRetentionService = new SendBuildRetention(buildRetention, buildName, project, async, log);
        sendBuildRetentionService.execute(jfrogHttpClient);
    }

    public void stageBuild(String buildName, String buildNumber, String project, Promotion promotion) throws IOException {
        StageBuild stageBuildService = new StageBuild(buildName, buildNumber, project, promotion, log);
        stageBuildService.execute(jfrogHttpClient);
    }

    public void updateFileProperty(String itemPath, String properties) throws IOException {
        UpdateFileProperty updateFilePropertyService = new UpdateFileProperty(itemPath, properties, log);
        updateFilePropertyService.execute(jfrogHttpClient);
    }

    public ArtifactoryUploadResponse upload(DeployDetails details) throws IOException {
        return upload(details, null, null);
    }

    public ArtifactoryUploadResponse upload(DeployDetails details, String logPrefix) throws IOException {
        return upload(details, logPrefix, null);
    }

    public ArtifactoryUploadResponse upload(DeployDetails details, String logPrefix, Integer minChecksumDeploySizeKb) throws IOException {
        Upload uploadService = new Upload(details, logPrefix, minChecksumDeploySizeKb, log);
        return uploadService.execute(jfrogHttpClient);
    }

    public void deleteRepository(String repository) throws IOException {
        DeleteRepository deleteRepositoryService = new DeleteRepository(repository, log);
        deleteRepositoryService.execute(jfrogHttpClient);
    }

    public void deleteRepositoryContent(String repository) throws IOException {
        DeleteRepositoryContent deleteRepositoryContentService = new DeleteRepositoryContent(repository, log);
        deleteRepositoryContentService.execute(jfrogHttpClient);
    }

    public void createRepository(String repository, String repositoryJsonConfig) throws IOException {
        CreateRepository createRepository = new CreateRepository(repository, repositoryJsonConfig, log);
        createRepository.execute(jfrogHttpClient);
    }

    public String getLatestBuildNumber(String buildName, String latestType, String project) throws IOException {
        if (!LATEST.equals(latestType.trim()) && LAST_RELEASE.equals(latestType.trim())) {
            log.warn("GetLatestBuildNumber accepts only two latest types: LATEST or LAST_RELEASE");
            return null;
        }
        Version versionService = new Version(log);
        if (versionService.execute(jfrogHttpClient).isOSS()) {
            throw new IllegalArgumentException(String.format("%s is not supported in Artifactory OSS.", latestType));
        }
        List<BuildPatternArtifactsRequest> artifactsRequest = Lists.newArrayList();
        artifactsRequest.add(new BuildPatternArtifactsRequest(buildName, latestType, project));
        List<BuildPatternArtifacts> artifactsResponses = retrievePatternArtifacts(artifactsRequest);
        // Artifactory returns null if no build was found
        if (artifactsResponses.get(0) != null) {
            return artifactsResponses.get(0).getBuildNumber();
        }
        return null;
    }
}
