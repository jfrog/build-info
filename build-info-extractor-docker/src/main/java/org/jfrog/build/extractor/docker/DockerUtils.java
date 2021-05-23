package org.jfrog.build.extractor.docker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class DockerUtils {
    /**
     * Get config digest from manifest (image id).
     */
    public static String getConfigDigest(String manifest) throws IOException {
        JsonNode manifestTree = createMapper().readTree(manifest);
        JsonNode schemaVersion = manifestTree.get("schemaVersion");
        if (schemaVersion == null) {
            throw new IllegalStateException("Could not find 'schemaVersion' in manifest");
        }
        if (schemaVersion.asInt() == 1) {
            throw new IllegalStateException("Docker build info is not supported for docker V1 images");
        }
        JsonNode config = manifestTree.get("config");
        if (config == null) {
            throw new IllegalStateException("Could not find 'config' in manifest");
        }
        JsonNode digest = config.get("digest");
        if (digest == null) {
            throw new IllegalStateException("Could not find config digest in manifest");
        }
        return StringUtils.remove(digest.toString(), "\"");
    }

    /**
     * Get the digest from fat-manifest according to os and arch.
     *
     * @param manifest - fat-manifest.
     * @param os       -      image os to search.
     * @param arch     -    arch to search.
     * @return digest related to os and arch. If not found return an empty string.
     * @throws IOException fat-manifest has missing 'manifest' key.
     */
    public static String getImageDigestFromFatManifest(String manifest, String os, String arch) throws IOException {
        if (StringUtils.isAnyBlank(os, arch)) {
            return StringUtils.EMPTY;
        }
        JsonNode fatManifestTree = createMapper().readTree(manifest);
        JsonNode manifests = fatManifestTree.get("manifests");
        if (manifests == null) {
            throw new IllegalStateException("Could not find 'manifests' in fat-manifest");
        }
        for (JsonNode manifestInfo : manifests) {
            JsonNode manifestInfoPlatform = manifestInfo.get("platform");
            if (manifestInfoPlatform == null) {
                continue;
            }

            JsonNode manifestOs = manifestInfoPlatform.get("os");
            JsonNode manifestArch = manifestInfoPlatform.get("architecture");
            if (manifestOs == null || manifestArch == null) {
                continue;
            }

            if (os.equals(manifestOs.asText()) && arch.equals(manifestArch.asText())) {
                return manifestInfo.get("digest").asText();
            }
        }
        return "";
    }

    /**
     * Create an object mapper for serialization/deserialization.
     * This mapper ignore unknown properties and null values.
     *
     * @return a new object mapper
     */
    public static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * Get a list of layer digests from docker manifest.
     */
    public static List<String> getLayersDigests(String manifestContent) throws IOException {
        List<String> dockerLayersDependencies = new ArrayList<>();
        JsonNode manifest = createMapper().readTree(manifestContent);
        JsonNode schemaVersion = manifest.get("schemaVersion");
        if (schemaVersion == null) {
            throw new IllegalStateException("Could not find 'schemaVersion' in manifest");
        }
        boolean isSchemeVersion1 = schemaVersion.asInt() == 1;
        JsonNode fsLayers = getFsLayers(manifest, isSchemeVersion1);
        for (JsonNode fsLayer : fsLayers) {
            if (!isForeignLayer(isSchemeVersion1, fsLayer)) {
                JsonNode blobSum = getBlobSum(isSchemeVersion1, fsLayer);
                dockerLayersDependencies.add(blobSum.asText());
            }
        }
        dockerLayersDependencies.add(getConfigDigest(manifestContent));
        //Add manifest sha1
        String manifestSha1 = toSha1(manifestContent);
        dockerLayersDependencies.add("sha1:" + manifestSha1);
        return dockerLayersDependencies;
    }

    private static String toSha1(String data) {
        try {
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
            msdDigest.update(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(msdDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to convert manifest.json content to SHA1.");
        }
    }

    /**
     * Return blob sum depend on scheme version.
     */
    private static JsonNode getFsLayers(JsonNode manifest, boolean isSchemeVersion1) {
        JsonNode fsLayers;
        if (isSchemeVersion1) {
            fsLayers = manifest.get("fsLayers");
        } else {
            fsLayers = manifest.get("layers");
        }
        if (fsLayers == null) {
            throw new IllegalStateException("Could not find 'fsLayers' or 'layers' in manifest");
        }
        return fsLayers;
    }

    private static boolean isForeignLayer(boolean isSchemeVersion1, JsonNode fsLayer) {
        return !isSchemeVersion1 &&
                fsLayer.get("mediaType") != null &&
                fsLayer.get("mediaType").asText().equals("application/vnd.docker.image.rootfs.foreign.diff.tar.gzip");
    }

    /**
     * Return blob sum depend on scheme version.
     */
    private static JsonNode getBlobSum(boolean isSchemeVersion1, JsonNode fsLayer) {
        JsonNode blobSum;
        if (isSchemeVersion1) {
            blobSum = fsLayer.get("blobSum");
        } else {
            blobSum = fsLayer.get("digest");
        }
        if (blobSum == null) {
            throw new IllegalStateException("Could not find 'blobSub' or 'digest' in manifest");
        }
        return blobSum;
    }

    /**
     * Get sha value from digest.
     * example: sha256:abcabcabc12334 the value is sha256.
     */
    public static String getShaVersion(String digest) {
        return StringUtils.substring(digest, 0, StringUtils.indexOf(digest, ":"));
    }

    /**
     * Get sha value from digest.
     * example: sha256:abcabcabc12334 the value is abcabcabc12334.
     */
    public static String getShaValue(String digest) {
        return StringUtils.substring(digest, StringUtils.indexOf(digest, ":") + 1);
    }

    /**
     * Digest format to layer file name.
     */
    public static String digestToFileName(String digest) {
        if (StringUtils.startsWith(digest, "sha1")) {
            return "manifest.json";
        }
        return getShaVersion(digest) + "__" + getShaValue(digest);
    }

    /**
     * Returns number of dependencies layers in the image.
     */
    public static int getNumberOfDependentLayers(String imageContent) throws IOException {
        JsonNode history = createMapper().readTree(imageContent).get("history");
        if (history == null) {
            throw new IllegalStateException("Could not find 'history' tag");
        }
        int layersNum = history.size();
        boolean newImageLayers = true;
        for (int i = history.size() - 1; i >= 0; i--) {
            if (newImageLayers) {
                layersNum--;
            }
            JsonNode layer = history.get(i);
            JsonNode emptyLayer = layer.get("empty_layer");
            if (!newImageLayers && emptyLayer != null) {
                layersNum--;
            }
            if (layer.get("created_by") == null) {
                continue;
            }
            String createdBy = layer.get("created_by").textValue();
            if (createdBy.contains("ENTRYPOINT") || createdBy.contains("MAINTAINER")) {
                newImageLayers = false;
            }
        }
        return layersNum;
    }

    /**
     * Layer file name to digest format.
     */
    public static String fileNameToDigest(String fileName) {
        return StringUtils.replace(fileName, "__", ":");
    }

    /**
     * Parse imageTag and get the relative path of the pushed image.
     * example: url:8081/image:version to image/version.
     */
    public static String getImagePath(String imageTag) {
        int indexOfSlash = imageTag.indexOf("/");
        int indexOfLastColon = imageTag.lastIndexOf(":");
        String imageName;
        String imageVersion;

        if (indexOfLastColon < 0 || indexOfLastColon < indexOfSlash) {
            imageName = imageTag.substring(indexOfSlash + 1);
            imageVersion = "latest";
        } else {
            imageName = imageTag.substring(indexOfSlash + 1, indexOfLastColon);
            imageVersion = imageTag.substring(indexOfLastColon + 1);
        }
        return imageName + "/" + imageVersion;
    }

    /**
     * Check for the version in docker image tag (used in Jenkins).
     */
    @SuppressWarnings("unused")
    public static Boolean isImageVersioned(String imageTag) {
        int indexOfFirstSlash = imageTag.indexOf("/");
        int indexOfLastColon = imageTag.lastIndexOf(":");
        return indexOfFirstSlash < indexOfLastColon;
    }

    /**
     * Download meta data from .marker layer in Artifactory.
     * As a result, the marker layer will transform to a regular docker layer (required to collect build info such as sha1, etc.).
     *
     * @param repo               - Repository from which to download the layer
     * @param imageName          - Image name to download
     * @param imageDigests       - image digest to download
     * @param artifactoryManager - Artifactory Manager
     */
    public static void downloadMarkerLayer(String repo, String imageName, String imageDigests, ArtifactoryManager artifactoryManager) throws IOException {
        String url = "/api/docker/" + repo + "/v2/" + imageName + "/blobs/" + imageDigests;
        artifactoryManager.downloadHeaders(url);
    }

    /**
     * @param imagePath - path to an image in artifactory without proxy e.g. image/image-tag
     * @param repo      - target repo to search
     * @param cmd       - docker push cmd/ docker pull cmd
     * @return All possible paths in Artifactory in order to find image manifest using proxy.
     */
    public static List<String> getArtManifestPath(String imagePath, String repo, CommandType cmd) {
        ArrayList<String> paths = new ArrayList<>();
        // Assuming reverse proxy e.g. ecosysjfrog-docker-local.jfrog.io.
        paths.add(repo + "/" + imagePath);

        // Assuming proxy-less e.g. orgab.jfrog.team/docker-local.
        paths.add(imagePath);

        int totalSlash = org.apache.commons.lang.StringUtils.countMatches(imagePath, "/");
        if (cmd == CommandType.Push || totalSlash > 3) {
            return paths;
        }
        // Assume reverse proxy - this time with 'library' as part of the path.
        paths.add(repo + "/library/" + imagePath);

        // Assume proxy-less - this time with 'library' as part of the path.
        int secondSlash = StringUtils.ordinalIndexOf(imagePath, "/", 2);
        paths.add(repo + "/library/" + imagePath.substring(secondSlash + 1));

        return paths;
    }

    public enum CommandType {
        Push,
        Pull
    }
}
