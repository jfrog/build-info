package org.jfrog.build.extractor.clientConfiguration.deploy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.client.DeployableArtifactDetail;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;

/**
 * Utilities for deployable artifacts.
 * Deployable artifacts file is a list of DeployableArtifactDetail.
 * The DeployDetails set is prepared in the build artifacts phase. From DeployDetails set we extract the list of DeployableArtifactDetail.
 * <p>
 * Created by yahavi on 25/04/2017.
 */
public class DeployableArtifactsUtils {

    public static void saveDeployableArtifactsToFile(Map<String, Set<DeployDetails>> deployableArtifactsByModule, File toFile, boolean saveBackwardCompatible) throws IOException {
        if (saveBackwardCompatible) {
            saveBackwardCompatibleDeployableArtifacts(deployableArtifactsByModule, toFile);
            return;
        }
        saveDeployableArtifactsByModule(deployableArtifactsByModule, toFile);
    }

    private static void saveDeployableArtifactsByModule(Map<String, Set<DeployDetails>> deployableArtifactsByModule, File toFile) throws IOException {
        Map<String, List<DeployableArtifactDetail>> deployableArtifactsDetails = new HashMap<>();
        deployableArtifactsByModule.forEach((module, deployableArtifacts) ->
                deployableArtifactsDetails.put(module, DeployableArtifactsUtils.getDeployableArtifactsPaths(deployableArtifacts)));
        ObjectMapper mapper = createMapper();
        mapper.writeValue(toFile, deployableArtifactsDetails);
    }

    /**
     * For backward compatibility, save the deployable artifacts as list (for pipelines using Gradle Artifactory Plugin with version 4.15.1 and above, along with Jenkins Artifactory Plugin bellow 3.6.1)
     */
    @Deprecated
    private static void saveBackwardCompatibleDeployableArtifacts(Map<String, Set<DeployDetails>> deployableArtifactsByModule, File toFile) throws IOException {
        List<DeployableArtifactDetail> deployableArtifactsList = new ArrayList<>();
        deployableArtifactsByModule.forEach((module, deployableArtifacts) ->
                deployableArtifactsList.addAll(DeployableArtifactsUtils.getDeployableArtifactsPaths(deployableArtifacts)));
        ObjectMapper mapper = createMapper();
        mapper.writeValue(toFile, deployableArtifactsList);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static Map<String, List<DeployableArtifactDetail>> loadDeployableArtifactsFromFile(File fromFile, File fromBackwardCompatibleFile) throws IOException {
        Map<String, List<DeployableArtifactDetail>> deployableArtifactsMap = loadDeployableArtifactsByModuleFromFile(fromFile);
        if (deployableArtifactsMap.isEmpty()) {
            return loadBackwardCompatibleDeployableArtifactsFromFile(fromBackwardCompatibleFile);
        }
        return deployableArtifactsMap;
    }

    private static Map<String, List<DeployableArtifactDetail>> loadDeployableArtifactsByModuleFromFile(File fromFile) throws IOException {
        if (fromFile == null || fromFile.length() == 0) {
            return new HashMap<>();
        }
        ObjectMapper mapper = createMapper();
        return mapper.readValue(fromFile, new TypeReference<Map<String, List<DeployableArtifactDetail>>>() {
        });
    }

    /**
     * For backwards compatibility, load the deployable artifacts as list (for pipelines using Gradle Artifactory Plugin with version bellow 4.15.0, along with Jenkins Artifactory Plugin 3.6.1 and above)
     */
    @Deprecated
    private static Map<String, List<DeployableArtifactDetail>> loadBackwardCompatibleDeployableArtifactsFromFile(File fromFile) throws IOException {
        if (fromFile == null || fromFile.length() == 0) {
            return new HashMap<>();
        }
        ObjectMapper mapper = createMapper();
        List<DeployableArtifactDetail> backwardCompatibleList = mapper.readValue(fromFile, new TypeReference<List<DeployableArtifactDetail>>() {
        });
        // Convert to map
        Map<String, List<DeployableArtifactDetail>> deployableArtifactMap = new HashMap<>();
        if (!backwardCompatibleList.isEmpty()) {
            deployableArtifactMap.put("", backwardCompatibleList);
        }
        return deployableArtifactMap;
    }

    private static List<DeployableArtifactDetail> getDeployableArtifactsPaths(Set<DeployDetails> deployDetails) {
        List<DeployableArtifactDetail> deployableArtifacts = new ArrayList<>();
        for (DeployDetails artifact : deployDetails) {
            Map<String, Collection<String>> artifactProps = artifact.getProperties() != null ? artifact.getProperties().asMap() : Collections.emptyMap();
            deployableArtifacts.add(new DeployableArtifactDetail(artifact.getFile().getAbsolutePath(), artifact.getArtifactPath(), artifact.getSha1(), artifact.getSha256(), artifact.getDeploySucceeded(), artifact.getTargetRepository(), artifactProps));
        }
        return deployableArtifacts;
    }
}
