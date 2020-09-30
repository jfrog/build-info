package org.jfrog.build.extractor.docker;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.hash.Hashing;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     * Create an object mapper for serialization/deserializaion.
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
        List<String> dockerLayersDependencies = new ArrayList<String>();
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
        String manifestSha1 = Hashing.sha1().hashString(manifestContent, Charsets.UTF_8).toString();
        dockerLayersDependencies.add("sha1:" + manifestSha1);
        return dockerLayersDependencies;
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
     *
     * @throws IOException
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
     * Converts the http entity to string. If entity is null, returns empty string.
     *
     * @throws IOException
     */
    public static String entityToString(HttpEntity entity) throws IOException {
        if (entity != null) {
            InputStream is = entity.getContent();
            return IOUtils.toString(is, "UTF-8");
        }
        return "";
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
     * Check for the version in docker image tag.
     */
    public static Boolean isImageVersioned(String imageTag) {
        int indexOfFirstSlash = imageTag.indexOf("/");
        int indexOfLastColon = imageTag.lastIndexOf(":");
        return indexOfFirstSlash < indexOfLastColon;
    }

    // Docker-Java uses the temp dir to Exe binaries, in some cases the default temp dir(especially in Linux os)
    // might have a NONEXE flag, therefore we override it with our build info extractor dir.
    public static void initTempDir(File path) {
        // Extract the dir path.
        String pathDir = path.getAbsoluteFile().getParent();
        // Set the Java temp dir system property. As a result, java will create it for us.
        System.setProperty("java.io.tmpdir", Paths.get(pathDir, "DockerJavaTemp").toString());
    }
}
