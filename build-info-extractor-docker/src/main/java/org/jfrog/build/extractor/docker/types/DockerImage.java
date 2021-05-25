package org.jfrog.build.extractor.docker.types;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.docker.DockerUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

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
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private String manifest;
    private String imagePath;
    private DockerLayers layers;

    public DockerImage(String imageId, String imageTag, String targetRepo, ArtifactoryManagerBuilder artifactoryManagerBuilder, String arch, String os) {
        this.imageId = imageId;
        this.imageTag = imageTag;
        this.targetRepo = targetRepo;
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
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
    private void checkAndSetManifestAndImagePathCandidates(String candidateManifestPath, ArtifactoryManager artifactoryManager, Log logger) throws IOException {
        Pair<String, String> candidateDetails = getManifestFromArtifactory(artifactoryManager, candidateManifestPath, logger);
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
     * @param artifactoryManager - Artifactory Manager.
     * @param manifestPath       - Path to manifest in Artifactory.
     * @return A pair of (manifest content, path to manifest).
     * @throws IOException fail to search for manifest json in manifestPath.
     */
    private Pair<String, String> getManifestFromArtifactory(ArtifactoryManager artifactoryManager, String manifestPath, Log logger) throws IOException {
        String pathWithoutRepo = StringUtils.substringAfter(manifestPath, "/");
        String downloadUrl = manifestPath + "/manifest.json";
        logger.info("Trying to download manifest from " + downloadUrl);
        try {
            return Pair.of(artifactoryManager.download(downloadUrl), pathWithoutRepo);
        } catch (Exception e) {
            if (artifactoryManager.isLocalRepo(targetRepo)) {
                throw e;
            }
            downloadUrl = manifestPath + "/list.manifest.json";
            logger.info("Fallback for remote/virtual repository. Trying to download fat-manifest from " + downloadUrl);
            String digestsFromFatManifest = DockerUtils.getImageDigestFromFatManifest(artifactoryManager.download(downloadUrl), os, architecture);
            if (digestsFromFatManifest.isEmpty()) {
                logger.info("Failed to get image digest from fat manifest");
                throw e;
            }
            logger.info("Found image digest from fat manifest. Trying to download the resulted manifest from path: " + manifestPath);
            // Remove the tag from the pattern, and place the manifest digest instead.
            manifestPath = StringUtils.substringBeforeLast(manifestPath, "/") + "/" + digestsFromFatManifest.replace(":", "__");
        }

        downloadUrl = manifestPath + "/manifest.json";
        logger.info("Trying to download manifest from " + downloadUrl);
        return Pair.of(artifactoryManager.download(downloadUrl), StringUtils.substringAfter(manifestPath, "/"));
    }

    private void setBuildInfoModuleProps(ModuleBuilder moduleBuilder) {
        buildInfoModuleProps.setProperty("docker.image.id", DockerUtils.getShaValue(imageId));
        buildInfoModuleProps.setProperty("docker.captured.image", imageTag);
        moduleBuilder.properties(buildInfoModuleProps);
    }

    private DockerLayers createLayers(ArtifactoryManager artifactoryManager, String aql) throws IOException {
        AqlSearchResult result = artifactoryManager.searchArtifactsByAql(aql);
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
    private void setDependenciesAndArtifacts(ModuleBuilder moduleBuilder, ArtifactoryManager artifactoryManager) throws IOException {
        DockerLayer historyLayer = layers.getByDigest(imageId);
        if (historyLayer == null) {
            throw new IllegalStateException("Could not find the history docker layer: " + imageId + " for image: " + imageTag + " in Artifactory.");
        }
        int dependencyLayerNum = DockerUtils.getNumberOfDependentLayers(artifactoryManager.download(historyLayer.getFullPath()));

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

    public Module generateBuildInfoModule(Log logger, DockerUtils.CommandType cmdType) throws IOException, InterruptedException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            ModuleBuilder moduleBuilder = new ModuleBuilder()
                    .type(ModuleType.DOCKER)
                    .id(imageTag.substring(imageTag.indexOf("/") + 1))
                    .repository(targetRepo);
            try {
                findAndSetManifestFromArtifactory(artifactoryManager, logger, cmdType);
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
                setDependenciesAndArtifacts(moduleBuilder, artifactoryManager);
            } else {
                setDependencies(moduleBuilder);
            }
            setBuildInfoModuleProps(moduleBuilder);
            return moduleBuilder.build();
        }
    }

    private void loadLayers(String manifestPath) throws IOException {
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            layers = getLayers(artifactoryManager, manifestPath);
            List<DockerLayer> markerLayers = layers.getLayers().stream().filter(layer -> layer.getFileName().endsWith(".marker")).collect(Collectors.toList());
            // Transform all marker layers into regular layer.
            if (markerLayers.size() > 0) {
                for (DockerLayer markerLayer : markerLayers) {
                    // Get image name without '.marker' suffix.
                    String imageDigests = StringUtils.removeEnd(markerLayer.getDigest(), ".marker");
                    String imageName = StringUtils.substringBetween(imageTag, "/", ":");
                    DockerUtils.downloadMarkerLayer(targetRepo, imageName, imageDigests, artifactoryManager);
                }
                layers = getLayers(artifactoryManager, manifestPath);
            }
        }
    }

    private DockerLayers getLayers(ArtifactoryManager artifactoryManager, String manifestPath) throws IOException {
        String searchableRepo = targetRepo;
        if (artifactoryManager.isRemoteRepo(targetRepo)) {
            searchableRepo += "-cache";
        }
        String aql = getAqlQuery(artifactoryManager.getVersion().isAtLeast(VIRTUAL_REPOS_SUPPORTED_VERSION), searchableRepo, manifestPath);
        return createLayers(artifactoryManager, aql);
    }

    /**
     * Find and validate manifest.json file in Artifactory for the current image.
     * Since provided imageTag differs between reverse-proxy and proxy-less configuration, try to build the correct manifest path.
     */
    private void findAndSetManifestFromArtifactory(ArtifactoryManager artifactoryManager, Log logger, DockerUtils.CommandType cmdType) throws IOException {
        // Try to get manifest, assuming reverse proxy
        String ImagePath = DockerUtils.getImagePath(imageTag);
        ArrayList<String> manifestPathCandidate = new ArrayList<>(DockerUtils.getArtManifestPath(ImagePath, targetRepo, cmdType));
        logger.info("Searching manifest for image \"" + imageTag + "\" in \"" + artifactoryManager.getUrl() + "\" under \"" + targetRepo + "\" repository");
        int listLen = manifestPathCandidate.size();
        for (int i = 0; i < listLen; i++) {
            try {
                logger.info("Searching manifest in path: " + manifestPathCandidate.get(i));
                checkAndSetManifestAndImagePathCandidates(manifestPathCandidate.get(i), artifactoryManager, logger);
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
