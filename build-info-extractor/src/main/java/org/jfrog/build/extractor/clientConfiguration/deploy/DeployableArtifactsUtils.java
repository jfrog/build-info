package org.jfrog.build.extractor.clientConfiguration.deploy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.jfrog.build.client.DeployableArtifactDetail;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Utilities for deployable artifacts.
 * Deployable artifacts file is a list of DeployableArtifactDetail.
 * The DeployDetails set is prepared in the build artifacts phase. From DeployDetails set we extract the list of DeployableArtifactDetail.
 *
 * Created by yahavi on 25/04/2017.
 */
public class DeployableArtifactsUtils {

    public static void saveDeployableArtifactsByModuleToFile(Map<String, Set<DeployDetails>> deployableArtifactsByModule, File toFile) throws IOException {
        Map<String, List<DeployableArtifactDetail>> deployableArtifactsDetails = new HashMap<>();
        deployableArtifactsByModule.forEach((module, deployableArtifacts) ->
                deployableArtifactsDetails.put(module, DeployableArtifactsUtils.getDeployableArtifactsPaths(deployableArtifacts)));
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(toFile, deployableArtifactsDetails);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static Map<String, List<DeployableArtifactDetail>> loadDeployableArtifactsByModuleFromFile(File fromFile) throws IOException {
        if (fromFile == null || fromFile.length() == 0) {
            return new HashMap<>();
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(fromFile, new TypeReference<Map<String, List<DeployableArtifactDetail>>>(){});
        } catch (MismatchedInputException e) {
            try {
                // For backwards compatibility (pipelines who us the native Artifactory plugin), try reading the json file as list
                List<DeployableArtifactDetail> deployableArtifactDetailList = mapper.readValue(fromFile, new TypeReference<List<DeployableArtifactDetail>>(){});
                // If successful, convert to map
                Map<String, List<DeployableArtifactDetail>> deployableArtifactMap = new HashMap<>();
                deployableArtifactMap.put("", deployableArtifactDetailList);
                return deployableArtifactMap;
            } catch (Exception secondTryException) {
                throw new RuntimeException("Failed loading deployable artifacts from file: ", e);
            }
        }
    }

    private static List<DeployableArtifactDetail> getDeployableArtifactsPaths(Set<DeployDetails> deployDetails) {
        List<DeployableArtifactDetail> deployableArtifacts = new ArrayList<DeployableArtifactDetail>();
        for (DeployDetails artifact : deployDetails) {
            deployableArtifacts.add(new DeployableArtifactDetail(artifact.getFile().getAbsolutePath(), artifact.getArtifactPath(), artifact.getSha1()));
        }
        return deployableArtifacts;
    }
}
