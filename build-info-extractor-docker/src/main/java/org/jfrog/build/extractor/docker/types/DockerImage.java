package org.jfrog.build.extractor.docker.types;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.builder.ModuleType;
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.jfrog.build.extractor.docker.DockerUtils.entityToString;

public class DockerImage implements Serializable {
    private final String imageId;
    private final String imageTag;
    private final String targetRepo;
    // Properties to be attached to the docker layers deployed to Artifactory.
    private final ArtifactoryVersion VIRTUAL_REPOS_SUPPORTED_VERSION = new ArtifactoryVersion("4.8.1");
    // List of properties added to the build-info generated for this docker image.
    private final Properties buildInfoModuleProps = new Properties();
    private final String os;
    private final String architecture;
    private final ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder;
    private final ArtifactoryDependenciesClientBuilder dependenciesClientBuilder;
    private String manifest;
    private String imagePath;
    private DockerLayers layers;

    public DockerImage(String imageId, String imageTag, String targetRepo, ArtifactoryBuildInfoClientBuilder buildInfoClientBuilder, ArtifactoryDependenciesClientBuilder dependenciesClientBuilder, String arch, String os) {
        this.imageId = imageId;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
        this.buildInfoClientBuilder = buildInfoClientBuilder;
        this.dependenciesClientBuilder = dependenciesClientBuilder;
        this.architecture = arch;
        this.os = os;
    }

    public DockerLayers getLayers() {
        return layers;
    }

    /**
     * Check if the provided manifestPath is correct.
     * Set the manifest and imagePath in case of the correct manifest.
     */
    private void checkAndSetManifestAndImagePathCandidates(String candidateManifestPath, ArtifactoryDependenciesClient dependenciesClient, Log logger) throws IOException {
        Pair<String, String> candidateDetails = getManifestFromArtifactory(dependenciesClient, candidateManifestPath, logger);
        String manifestContent = candidateDetails.getLeft();
        String manifestPath = candidateDetails.getRight();
        String imageDigest = DockerUtils.getConfigDigest(manifestContent);
        if (imageDigest.equals(imageId)) {
            manifest = manifestContent;
            imagePath = manifestPath;
            loadLayers(manifestPath);
        }
    }

    /**
     * Search for 'manifest.json' at 'manifestPath' in artifactory and return its content.
     * If the target repository is not local type , fat-manifest can be found instead, which is a list of 'manifest.json' digest for one or more platforms.
     * In order to find the right digest, we read and iterate on each digest and search for the one with the same os and arch.
     * By using the correct digest from the fat-manifest, we are able to build the path toward manifest.json in Artifactory.
     *
     * @param dependenciesClient - Dependencies client builder.
     * @param manifestPath       - Path to manifest in Artifactory.
     * @return A pair of (manifest content, path to manifest).
     * @throws IOException fail to search for manifest json in manifestPath.
     */
    private Pair<String, String> getManifestFromArtifactory(ArtifactoryDependenciesClient dependenciesClient, String manifestPath, Log logger) throws IOException {
        String artUrl = dependenciesClient.getArtifactoryUrl() + "/";
        String pathWithoutRepo = StringUtils.substringAfter(manifestPath, "/");
        HttpEntity entity = null;
        String downloadUrl = artUrl + manifestPath + "/manifest.json";
        logger.info("Trying to download manifest from " + downloadUrl);
        try (CloseableHttpResponse response = dependenciesClient.downloadArtifact(downloadUrl)) {
            entity = response.getEntity();
            return Pair.of(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8), pathWithoutRepo);
        } catch (Exception e) {
            if (dependenciesClient.isLocalRepo(targetRepo)) {
                throw e;
            }
            EntityUtils.consume(entity);
            downloadUrl = artUrl + manifestPath + "/list.manifest.json";
            logger.info("Fallback for local/virtual repository. Trying to download manifest from " + downloadUrl);
            try (CloseableHttpResponse response = dependenciesClient.downloadArtifact(downloadUrl)) {
                logger.info("Fallback for local/virtual repository. Trying to download manifest from " + downloadUrl);
                entity = response.getEntity();
                String digestsFromFatManifest = DockerUtils.getImageDigestFromFatManifest(entityToString(response.getEntity()), os, architecture);
                if (digestsFromFatManifest.isEmpty()) {
                    logger.info("Fallback step, Failed to get image digest from fat manifest");
                    throw e;
                }
                // Remove the tag from the pattern, and place the manifest digest instead.
                logger.info("Found image digest from fat manifest. Trying to download the resulted manifest from path: " + manifestPath);
                manifestPath = StringUtils.substringBeforeLast(manifestPath, "/") + "/" + digestsFromFatManifest.replace(":", "__");
            }

            EntityUtils.consume(entity);
            try (CloseableHttpResponse response = dependenciesClient.downloadArtifact(artUrl + manifestPath + "/manifest.json")) {
                entity = response.getEntity();
                pathWithoutRepo = StringUtils.substringAfter(manifestPath, "/");
                return Pair.of(IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8), pathWithoutRepo);
            }
        } finally {
            EntityUtils.consumeQuietly(entity);
        }
    }

    private void setBuildInfoModuleProps(ModuleBuilder moduleBuilder) {
        buildInfoModuleProps.setProperty("docker.image.id", DockerUtils.getShaValue(imageId));
        buildInfoModuleProps.setProperty("docker.captured.image", imageTag);
        moduleBuilder.properties(buildInfoModuleProps);
    }

    private DockerLayers createLayers(ArtifactoryDependenciesClient dependenciesClient, String aql) throws IOException {
        AqlSearchResult result = dependenciesClient.searchArtifactsByAql(aql);
        DockerLayers layers = new DockerLayers();
        for (AqlSearchResult.SearchEntry entry : result.getResults()) {
            DockerLayer layer = new DockerLayer(entry);
            layers.addLayer(layer);
        }
        if (layers.getLayers().size() == 0) {
            throw new IllegalStateException(String.format("No docker layers found in Artifactory using AQL: %s after filtering layers in repos other than %s and with path other than %s", aql, targetRepo, imagePath));
        }
        return layers;
    }

    /**
     * Search the docker image in Artifactory and add all artifacts & dependencies into Module.
     */
    private void setDependenciesAndArtifacts(ModuleBuilder moduleBuilder, ArtifactoryDependenciesClient dependenciesClient) throws IOException {
        DockerLayer historyLayer = layers.getByDigest(imageId);
        if (historyLayer == null) {
            throw new IllegalStateException("Could not find the history docker layer: " + imageId + " for image: " + imageTag + " in Artifactory.");
        }
        HttpEntity entity = null;
        try (CloseableHttpResponse res = dependenciesClient.downloadArtifact(dependenciesClient.getArtifactoryUrl() + "/" + historyLayer.getFullPath())) {
            entity = res.getEntity();
            int dependencyLayerNum = DockerUtils.getNumberOfDependentLayers(entityToString(res.getEntity()));

            LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
            LinkedHashSet<Artifact> artifacts = new LinkedHashSet<>();
            // Filter out duplicate layers from manifest by using HashSet.
            // Docker manifest may hold 'empty layers', as a result, docker promote will fail to promote the same layer more than once.
            Iterator<String> it = DockerUtils.getLayersDigests(manifest).iterator();
            for (int i = 0; i < dependencyLayerNum; i++) {
                String digest = it.next();
                DockerLayer layer = layers.getByDigest(digest);
                Dependency dependency = new DependencyBuilder().id(layer.getFileName()).sha1(layer.getSha1()).build();
                dependencies.add(dependency);
                Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).remotePath(layer.getPath()).build();
                artifacts.add(artifact);
            }
            moduleBuilder.dependencies(new ArrayList<>(dependencies));
            while (it.hasNext()) {
                String digest = it.next();
                DockerLayer layer = layers.getByDigest(digest);
                if (layer == null) {
                    continue;
                }
                Artifact artifact = new ArtifactBuilder(layer.getFileName()).sha1(layer.getSha1()).remotePath(layer.getPath()).build();
                artifacts.add(artifact);
            }
            moduleBuilder.artifacts(new ArrayList<>(artifacts));
        } finally {
            EntityUtils.consume(entity);
        }
    }

    private void setDependencies(ModuleBuilder moduleBuilder) throws IOException {
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        // Filter out duplicate layers from manifest by using HashSet.
        // Docker manifest may hold 'empty layers', as a result, docker promote will fail to promote the same layer more than once.
        for (String digest : DockerUtils.getLayersDigests(manifest)) {
            DockerLayer layer = layers.getByDigest(digest);
            Dependency dependency = new DependencyBuilder().id(layer.getFileName()).sha1(layer.getSha1()).build();
            dependencies.add(dependency);
        }
        moduleBuilder.dependencies(new ArrayList<>(dependencies));
    }

    /**
     * Prepare AQL query to get all the manifest layers from Artifactory.
     * Needed for build-info sha1/md5 checksum for each artifact and dependency.
     */
    private String getAqlQuery(boolean includeVirtualRepos, String Repo, String manifestPath) {
        StringBuilder aqlRequestForDockerSha = new StringBuilder("items.find({")
                .append("\"path\":\"").append(manifestPath).append("\",")
                .append("\"repo\":\"").append(Repo).append("\"})");
        if (includeVirtualRepos) {
            aqlRequestForDockerSha.append(".include(\"name\",\"repo\",\"path\",\"actual_sha1\",\"virtual_repos\")");
        } else {
            aqlRequestForDockerSha.append(".include(\"name\",\"repo\",\"path\",\"actual_sha1\")");
        }
        return aqlRequestForDockerSha.toString();
    }

    public Module generateBuildInfoModule(Log logger, DockerUtils.CommandType cmdType) throws IOException {
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            ModuleBuilder moduleBuilder = new ModuleBuilder()
                    .type(ModuleType.DOCKER)
                    .id(imageTag.substring(imageTag.indexOf("/") + 1))
                    .repository(targetRepo);
            try {
                findAndSetManifestFromArtifactory(dependenciesClient, logger, cmdType);
            } catch (IOException e) {
                // The manifest could not be found in Artifactory.
                // Yet, we do not fail the build, but return an empty build-info instead.
                // The reason for not failing build is that there's a chance that the image was replaced
                // with another image, deployed to the same repo path.
                // This can happen if two parallel jobs build the same image. In that case, the build-info
                // for this build will be empty.
                logger.error("The manifest could not be fetched from Artifactory.");
                return moduleBuilder.build();
            }
            logger.info("Fetching details of published docker layers from Artifactory...");
            if (cmdType == DockerUtils.CommandType.Push) {
                setDependenciesAndArtifacts(moduleBuilder, dependenciesClient);
            } else {
                setDependencies(moduleBuilder);
            }
            setBuildInfoModuleProps(moduleBuilder);
            return moduleBuilder.build();
        }
    }

    private void loadLayers(String manifestPath) throws IOException {
        try (ArtifactoryBuildInfoClient buildInfoClient = buildInfoClientBuilder.build();
             ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            layers = getLayers(dependenciesClient, buildInfoClient, manifestPath);
            List<DockerLayer> markerLayers = layers.getLayers().stream().filter(layer -> layer.getFileName().endsWith(".marker")).collect(Collectors.toList());
            // Transform all marker layers into regular layer.
            if (markerLayers.size() > 0) {
                for (DockerLayer markerLayer : markerLayers) {
                    // Get image name without '.marker' suffix.
                    String imageDigests = StringUtils.removeEnd(markerLayer.getDigest(), ".marker");
                    String imageName = StringUtils.substringBetween(imageTag, "/", ":");
                    DockerUtils.downloadMarkerLayer(targetRepo, imageName, imageDigests, dependenciesClient);
                }
                layers = getLayers(dependenciesClient, buildInfoClient, manifestPath);
            }
        }
    }

    private DockerLayers getLayers(ArtifactoryDependenciesClient dependenciesClient, ArtifactoryBuildInfoClient buildInfoClient, String manifestPath) throws IOException {
        String searchableRepo = targetRepo;
        if (dependenciesClient.isRemoteRepo(targetRepo)) {
            searchableRepo += "-cache";
        }
        String aql = getAqlQuery(buildInfoClient.getArtifactoryVersion().isAtLeast(VIRTUAL_REPOS_SUPPORTED_VERSION), searchableRepo, manifestPath);
        return createLayers(dependenciesClient, aql);
    }

    /**
     * Find and validate manifest.json file in Artifactory for the current image.
     * Since provided imageTag differs between reverse-proxy and proxy-less configuration, try to build the correct manifest path.
     */
    private void findAndSetManifestFromArtifactory(ArtifactoryDependenciesClient dependenciesClient, Log logger, DockerUtils.CommandType cmdType) throws IOException {
        // Try to get manifest, assuming reverse proxy
        String ImagePath = DockerUtils.getImagePath(imageTag);
        ArrayList<String> manifestPathCandidate = new ArrayList<>(DockerUtils.getArtManifestPath(ImagePath, targetRepo, cmdType));
        logger.info("Searching manifest for image \"" + imageTag + "\" in \"" + dependenciesClient.getArtifactoryUrl() + "\" under \"" + targetRepo + "\" repository");
        int listLen = manifestPathCandidate.size();
        for (int i = 0; i < listLen; i++) {
            try {
                logger.info("Searching manifest in path: " + manifestPathCandidate.get(i));
                checkAndSetManifestAndImagePathCandidates(manifestPathCandidate.get(i), dependenciesClient, logger);
                return;
            } catch (IOException e) {
                // Throw the exception only if we reached the end of the loop, which means we tried all options.
                logger.info("The search failed with \"" + e.getMessage() + "\".");
                if (i == listLen - 1) {
                    throw e;
                }
            }
        }
    }
}
