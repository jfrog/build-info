package org.jfrog.build.extractor.docker.types;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
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
    private void checkAndSetManifestAndImagePathCandidates(String manifestPath, String candidateImagePath, ArtifactoryDependenciesClient dependenciesClient, boolean isLocalRepo) throws IOException {
        String candidateManifest = getManifestFromArtifactory(dependenciesClient, manifestPath, isLocalRepo);
        String imageDigest = DockerUtils.getConfigDigest(candidateManifest);
        if (imageDigest.equals(imageId)) {
            manifest = candidateManifest;
            imagePath = candidateImagePath;
            loadLayers();
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
     * @param isLocalRepo        - Indicates if the search is against a local repository.
     * @return The manifest content, otherwise throw an error.
     * @throws IOException fail to search for manifest json in manifestPath.
     */
    private String getManifestFromArtifactory(ArtifactoryDependenciesClient dependenciesClient, String manifestPath, boolean isLocalRepo) throws IOException {
        HttpResponse res = null;
        try {
            res = dependenciesClient.downloadArtifact(manifestPath + "/manifest.json");
            return IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            if (isLocalRepo) {
                throw e;
            }
            res = dependenciesClient.downloadArtifact(manifestPath + "/list.manifest.json");
            String digestsFromFatManifest = DockerUtils.getImageDigestFromFatManifest(entityToString(res.getEntity()), os, architecture);
            if (digestsFromFatManifest.isEmpty()) {
                throw e;
            }
            // Remove the tag from the pattern, and place the manifest digest instead.
            res = dependenciesClient.downloadArtifact(StringUtils.substringBeforeLast(manifestPath, "/") + "/" + digestsFromFatManifest.replace(":", "__") + "/manifest.json");
            return IOUtils.toString(res.getEntity().getContent(), StandardCharsets.UTF_8);
        } finally {
            if (res != null) {
                EntityUtils.consume(res.getEntity());
            }
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
        HttpResponse res = dependenciesClient.downloadArtifact(dependenciesClient.getArtifactoryUrl() + "/" + historyLayer.getFullPath());
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
    }

    private void setDependencies(Module buildInfoModule) throws IOException {
        LinkedHashSet<Dependency> dependencies = new LinkedHashSet<>();
        // Filter out duplicate layers from manifest by using HashSet.
        // Docker manifest may hold 'empty layers', as a result, docker promote will fail to promote the same layer more than once.
        for (String digest : DockerUtils.getLayersDigests(manifest)) {
            DockerLayer layer = layers.getByDigest(digest);
            Dependency dependency = new DependencyBuilder().id(layer.getFileName()).sha1(layer.getSha1()).build();
            dependencies.add(dependency);
        }
        buildInfoModule.setDependencies(new ArrayList<>(dependencies));
    }

    /**
     * Prepare AQL query to get all the manifest layers from Artifactory.
     * Needed for build-info sha1/md5 checksum for each artifact and dependency.
     */
    private String getAqlQuery(boolean includeVirtualRepos, String Repo) throws IOException {
        List<String> layersDigest = DockerUtils.getLayersDigests(manifest);
        StringBuilder aqlRequestForDockerSha = new StringBuilder("items.find({")
                .append("\"repo\":\"").append(Repo).append("\",\"$or\":[");
        List<String> layersQuery = new ArrayList<>();
        for (String digest : layersDigest) {
            String shaVersion = DockerUtils.getShaVersion(digest);
            String singleFileQuery;
            if (StringUtils.equalsIgnoreCase(shaVersion, "sha1")) {
                singleFileQuery = String.format("{\"actual_sha1\":\"%s\"}", DockerUtils.getShaValue(digest));
            } else {
                // Use wildcard pattern to collect layers with '.marker' suffix at the end.
                singleFileQuery = String.format("{\"name\":{\"$match\":\"%s*\"}}", DockerUtils.digestToFileName(digest));
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

    public Module generateBuildInfoModule(Log logger, DockerUtils.CommandType cmdType) throws IOException {
        try (ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            ModuleBuilder moduleBuilder = new ModuleBuilder()
            .type(ModuleType.DOCKER)
            .id(imageTag.substring(imageTag.indexOf("/") + 1))
            .repository(targetRepo);
            try {
                findAndSetManifestFromArtifactory(dependenciesClient.getArtifactoryUrl(), dependenciesClient, logger, cmdType);
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

    private void loadLayers() throws IOException {
        try (ArtifactoryBuildInfoClient buildInfoClient = buildInfoClientBuilder.build();
             ArtifactoryDependenciesClient dependenciesClient = dependenciesClientBuilder.build()) {
            layers = getLayers(dependenciesClient, buildInfoClient);
            List<DockerLayer> markerLayers = layers.getLayers().stream().filter(layer -> layer.getFileName().endsWith(".marker")).collect(Collectors.toList());
            // Transform all marker layers into regular layer.
            if (markerLayers.size() > 0) {
                for (DockerLayer markerLayer : markerLayers) {
                    // Get image name without '.marker' suffix.
                    String imageDigests = StringUtils.removeEnd(markerLayer.getDigest(), ".marker");
                    String imageName = StringUtils.substringBetween(imageTag, "/", ":");
                    DockerUtils.downloadMarkerLayer(targetRepo, imageName, imageDigests, dependenciesClient);
                }
                layers = getLayers(dependenciesClient, buildInfoClient);
            }
        }
    }

    private DockerLayers getLayers(ArtifactoryDependenciesClient dependenciesClient, ArtifactoryBuildInfoClient buildInfoClient) throws IOException {
        String searchableRepo = targetRepo;
        if (dependenciesClient.isRemoteRepo(targetRepo)) {
            searchableRepo += "-cache";
        }
        String aql = getAqlQuery(buildInfoClient.getArtifactoryVersion().isAtLeast(VIRTUAL_REPOS_SUPPORTED_VERSION), searchableRepo);
        return createLayers(dependenciesClient, aql);
    }

    /**
     * Find and validate manifest.json file in Artifactory for the current image.
     * Since provided imageTag differs between reverse-proxy and proxy-less configuration, try to build the correct manifest path.
     */
    private void findAndSetManifestFromArtifactory(String url, ArtifactoryDependenciesClient dependenciesClient, Log logger, DockerUtils.CommandType cmdType) throws IOException {
        // Try to get manifest, assuming reverse proxy
        String proxyImagePath = DockerUtils.getImagePath(imageTag);
        boolean isLocalRepo = dependenciesClient.isLocalRepo(targetRepo);
        String proxyManifestPath = StringUtils.join(new String[]{url, targetRepo, proxyImagePath}, "/");
        try {
            logger.info("Trying to fetch manifest from Artifactory, assuming reverse proxy configuration.");
            checkAndSetManifestAndImagePathCandidates(proxyManifestPath, proxyImagePath, dependenciesClient, isLocalRepo);
            return;
        } catch (IOException e) {
            logger.error("The manifest could not be fetched from Artifactory, assuming reverse proxy configuration - " + e.getMessage());
            // Ignore - Artifactory may have a proxy-less setup. Let's try that.
        }
        // Try to get manifest, assuming proxy-less
        String proxyLessImagePath = proxyImagePath.substring(proxyImagePath.indexOf("/") + 1);
        String proxyLessManifestPath = StringUtils.join(new String[]{url, targetRepo, proxyLessImagePath}, "/");
        logger.info("Trying to fetch manifest from Artifactory, assuming proxy-less configuration.");
        try {
            checkAndSetManifestAndImagePathCandidates(proxyLessManifestPath, proxyLessImagePath, dependenciesClient, isLocalRepo);
        } catch (IOException e) {
            logger.error("The manifest could not be fetched from Artifactory, assuming proxy-lessess configuration - " + e.getMessage());
            // If image path includes more than 3 slashes, Artifactory doesn't store this image under 'library',
            // thus we should not look further.
            int totalSlash = StringUtils.countMatches(proxyImagePath, "/");
            if (cmdType == DockerUtils.CommandType.Push || totalSlash > 3) {
                throw e;
            }
        }
        // Assume proxy-less - this time with 'library' as part of the path.
        proxyManifestPath = StringUtils.join(new String[]{url, targetRepo, "library", proxyImagePath}, "/");
        try {
            logger.info("Trying to fetch manifest from Artifactory, assuming reverse proxy configuration. This time with 'library' as part of the path");
            checkAndSetManifestAndImagePathCandidates(proxyManifestPath, proxyImagePath, dependenciesClient, isLocalRepo);
            return;
        } catch (IOException e) {
            logger.error("The manifest could not be fetched from Artifactory, assuming reverse proxy configuration - " + e.getMessage());
        }
        // Assume proxy-less - this time with 'library' as part of the path.
        proxyLessManifestPath = StringUtils.join(new String[]{url, targetRepo, "library", proxyLessImagePath}, "/");
        checkAndSetManifestAndImagePathCandidates(proxyLessManifestPath, proxyLessImagePath, dependenciesClient, isLocalRepo);
    }
}
