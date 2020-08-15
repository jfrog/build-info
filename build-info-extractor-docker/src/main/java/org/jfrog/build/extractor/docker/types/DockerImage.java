package org.jfrog.build.extractor.docker.types;

import com.google.common.collect.Sets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryVersion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.docker.DockerUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

public class DockerImage implements Serializable {
    private final String imageId;
    private final String imageTag;
    private final String targetRepo;
    // Properties to be attached to the docker layers deployed to Artifactory.
    private final ArtifactoryVersion VIRTUAL_REPOS_SUPPORTED_VERSION = new ArtifactoryVersion("4.8.1");
    private String manifest;
    private String agentName = "";
    // List of properties added to the build-info generated for this docker image.
    private Properties buildInfoModuleProps = new Properties();
    private String imagePath;

    public DockerImage(String imageId, String imageTag, String targetRepo) {
        this.imageId = imageId;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
    }

    /**
     * Check if the provided manifestPath is correct.
     * Set the manifest and imagePath in case of the correct manifest.
     *
     * @param manifestPath
     * @param candidateImagePath
     * @param dependenciesClient
     * @throws IOException
     */
    private void checkAndSetManifestAndImagePathCandidates(String manifestPath, String candidateImagePath, ArtifactoryDependenciesClient dependenciesClient) throws IOException {
        String candidateManifest = getManifestFromArtifactory(dependenciesClient, manifestPath);
        String imageDigest = DockerUtils.getConfigDigest(candidateManifest);
        if (imageDigest.equals(imageId)) {
            manifest = candidateManifest;
            imagePath = candidateImagePath;
        }
    }

    private String getManifestFromArtifactory(ArtifactoryDependenciesClient dependenciesClient, String manifestPath) throws IOException {
        HttpResponse res = null;
        try {
            res = dependenciesClient.downloadArtifact(manifestPath);
            return IOUtils.toString(res.getEntity().getContent());
        } finally {
            if (res != null) {
                EntityUtils.consume(res.getEntity());
            }
        }
    }

    private void setBuildInfoModuleProps(Module buildInfoModule) {
        buildInfoModuleProps.setProperty("docker.image.id", DockerUtils.getShaValue(imageId));
        buildInfoModuleProps.setProperty("docker.captured.image", imageTag);
        buildInfoModule.setProperties(buildInfoModuleProps);
    }

    private DockerLayers createLayers(ArtifactoryDependenciesClient dependenciesClient, String aql) throws IOException {
        AqlSearchResult result = dependenciesClient.searchArtifactsByAql(aql);
        DockerLayers layers = new DockerLayers();
        for (AqlSearchResult.SearchEntry entry : result.getResults()) {
            // Filtering out results with the wrong path.
            if (!StringUtils.equals(entry.getPath(), imagePath)) {
                continue;
            }
            // Filtering out results with the wrong repository.
            if (!StringUtils.equals(entry.getRepo(), targetRepo)) {
                Set<String> virtual_repos = Sets.newHashSet(entry.getVirtualRepos());
                if (!virtual_repos.contains(targetRepo)) {
                    continue;
                }
            }
            DockerLayer layer = new DockerLayer(entry);
            layers.addLayer(layer);
        }
        if (layers.getLayers().size() == 0) {
            throw new IllegalStateException(String.format("No docker layers found in Artifactory using AQL: %s after filtering layers in repos other than %s and with path other than %s", aql, targetRepo, imagePath));
        }
        return layers;
    }

    /**
     * Search the docker image in Artifactory and update each layer's properties with artifactsPropsStr.
     * Add all artifacts & dependencies into Module.
     *
     * @param buildInfoModule
     * @param properties
     * @param artifactsPropsStr
     * @param dependenciesClient
     * @param propertyChangeClient
     * @throws IOException
     */
    private void setDependenciesAndArtifacts(Module buildInfoModule, Properties properties, String artifactsPropsStr, ArtifactoryDependenciesClient dependenciesClient, ArtifactoryBuildInfoClient propertyChangeClient) throws IOException {
        String aql = getAqlQuery(propertyChangeClient);
        DockerLayers layers = createLayers(dependenciesClient, aql);
        DockerLayer historyLayer = layers.getByDigest(imageId);
        if (historyLayer == null) {
            throw new IllegalStateException("Could not find the history docker layer: " + imageId + " for image: " + imageTag + " in Artifactory.");
        }
        HttpResponse res = dependenciesClient.downloadArtifact(dependenciesClient.getArtifactoryUrl() + "/" + historyLayer.getFullPath());
        int dependencyLayerNum = DockerUtils.getNumberOfDependentLayers(DockerUtils.entityToString(res.getEntity()));
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        LinkedHashSet<Artifact> artifacts = new LinkedHashSet<>();
        // Filter out duplicate layers from manifest by using HashSet.
        // Docker manifest may hold 'empty layers', as a result, docker promote will fail to promote the same layer more than once.
        Iterator<String> it = DockerUtils.getLayersDigests(manifest).iterator();
        for (int i = 0; i < dependencyLayerNum; i++) {
            String digest = it.next();
            DockerLayer layer = layers.getByDigest(digest);
            HttpResponse httpResponse = propertyChangeClient.executeUpdateFileProperty(layer.getFullPath(), artifactsPropsStr);
            validateResponse(httpResponse);
            Dependency dependency = new DependencyBuilder().id(layer.getFileName()).sha1(layer.getSha1()).addProperty(properties).build();
            dependencies.add(dependency);
            Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).addProperty(properties).build();
            artifacts.add(artifact);
        }
        buildInfoModule.setDependencies(new ArrayList<>(dependencies));
        while (it.hasNext()) {
            String digest = it.next();
            DockerLayer layer = layers.getByDigest(digest);
            if (layer == null) {
                continue;
            }
            HttpResponse httpResponse = propertyChangeClient.executeUpdateFileProperty(layer.getFullPath(), artifactsPropsStr);
            validateResponse(httpResponse);
            Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).addProperty(properties).build();
            artifacts.add(artifact);
        }
        buildInfoModule.setArtifacts(new ArrayList<>(artifacts));
    }

    /**
     * Prepare AQL query to get all the manifest layers from Artifactory.
     * Needed for build-info sha1/md5 checksum for each artifact and dependency.
     *
     * @return
     * @throws IOException
     */
    private String getAqlQuery(ArtifactoryBuildInfoClient propertyChangeClient) throws IOException {
        boolean includeVirtualRepos = propertyChangeClient.getArtifactoryVersion().isAtLeast(VIRTUAL_REPOS_SUPPORTED_VERSION);
        List<String> layersDigest = DockerUtils.getLayersDigests(manifest);
        StringBuilder aqlRequestForDockerSha = new StringBuilder("items.find({")
                .append("\"path\":\"").append(imagePath).append("\",\"$or\":[");
        List<String> layersQuery = new ArrayList<String>();
        for (String digest : layersDigest) {
            String shaVersion = DockerUtils.getShaVersion(digest);
            String singleFileQuery;
            if (StringUtils.equalsIgnoreCase(shaVersion, "sha1")) {
                ;
                singleFileQuery = String.format("{\"actual_sha1\": \"%s\"}", DockerUtils.getShaValue(digest));
            } else {
                singleFileQuery = String.format("{\"name\": \"%s\"}", DockerUtils.digestToFileName(digest));
            }
            layersQuery.add(singleFileQuery);
        }
        aqlRequestForDockerSha.append(StringUtils.join(layersQuery, ","));
        if (includeVirtualRepos) {
            aqlRequestForDockerSha.append("]}).include(\"name\",\"repo\",\"path\",\"actual_sha1\",\"virtual_repos\")");
        } else {
            aqlRequestForDockerSha.append("]}).include(\"name\",\"repo\",\"path\",\"actual_sha1\")");
        }
        return aqlRequestForDockerSha.toString();
    }

    public Module generateBuildInfoModule(ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, ArtifactoryDependenciesClientBuilder dependenciesClientBuilder, Log logger, Map<String, String> artifactProperties) throws IOException {
        Properties buildInfoItemsProps = getBuildInfoProps(artifactProperties);
        String artifactsPropsStr = DockerUtils.buildPropertiesString(artifactProperties);
        try (ArtifactoryBuildInfoClient buildInfoClient = buildInfoClientBuilder.build();
             ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            Module buildInfoModule = new Module();
            buildInfoModule.setId(imageTag.substring(imageTag.indexOf("/") + 1));
            try {
                findAndSetManifestFromArtifactory(dependenciesClient.getArtifactoryUrl(), dependenciesClient, logger);
            } catch (IOException e) {
                // The manifest could be found in Artifactory.
                // Yet, we do not fail the build, but return an empty build-info instead.
                // The reason for not failing build is that there's a chance that the image was replaced
                // with another image, deployed to the same repo path.
                // This can happen if two parallel jobs build the same image. In that case, the build-info
                // for this build will be empty.
                logger.error("The manifest could not be fetched from Artifactory.");
                return buildInfoModule;
            }
            logger.info("Fetching details of published docker layers from Artifactory...");
            setDependenciesAndArtifacts(buildInfoModule, buildInfoItemsProps, artifactsPropsStr,
                    dependenciesClient, buildInfoClient);
            setBuildInfoModuleProps(buildInfoModule);
            return buildInfoModule;
        }
    }

    /**
     * Find and validate manifest.json file in Artifactory for the current image.
     * Since provided imageTag differs between reverse-proxy and proxy-less configuration, try to build the correct manifest path.
     *
     * @return
     * @throws IOException
     */
    private void findAndSetManifestFromArtifactory(String url, ArtifactoryDependenciesClient dependenciesClient, Log logger) throws IOException {
        // Try to get manifest, assuming reverse proxy
        String proxyImagePath = DockerUtils.getImagePath(imageTag);
        String proxyManifestPath = StringUtils.join(new String[]{url, targetRepo, proxyImagePath, "manifest.json"}, "/");
        try {
            logger.info("Trying to fetch manifest from Artifactory, assuming reverse proxy configuration.");
            checkAndSetManifestAndImagePathCandidates(proxyManifestPath, proxyImagePath, dependenciesClient);
            return;
        } catch (IOException e) {
            logger.error("The manifest could not be fetched from Artifactory, assuming reverse proxy configuration - " + e.getMessage());
            // Ignore - Artifactory may have a proxy-less setup. Let's try that.
        }
        // Try to get manifest, assuming proxy-less
        String proxyLessImagePath = proxyImagePath.substring(proxyImagePath.indexOf("/") + 1);
        String proxyLessManifestPath = StringUtils.join(new String[]{url, targetRepo, proxyLessImagePath, "manifest.json"}, "/");
        logger.info("Trying to fetch manifest from Artifactory, assuming proxy-less configuration.");
        checkAndSetManifestAndImagePathCandidates(proxyLessManifestPath, proxyLessImagePath, dependenciesClient);
    }

    private void validateResponse(HttpResponse httpResponse) throws IOException {
        int code = httpResponse.getStatusLine().getStatusCode();
        if (code != 204) {
            String response = DockerUtils.entityToString(httpResponse.getEntity());
            throw new IOException("Failed while trying to set properties on docker layer: " + response);
        }
    }

    private Properties getBuildInfoProps(Map<String, String> artifactProperties) {
        Properties buildInfoItemsProps = new Properties();
        if (artifactProperties != null) {
            buildInfoItemsProps.setProperty("build.name", artifactProperties.get("build.name"));
            buildInfoItemsProps.setProperty("build.number", artifactProperties.get("build.number"));
            buildInfoItemsProps.setProperty("build.timestamp", artifactProperties.get("build.timestamp"));
        }
        return buildInfoItemsProps;
    }
}
