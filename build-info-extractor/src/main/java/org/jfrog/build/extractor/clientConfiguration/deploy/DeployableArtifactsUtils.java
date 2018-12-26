package org.jfrog.build.extractor.clientConfiguration.deploy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jfrog.build.client.DeployableArtifactDetail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utilities for deployable artifacts.
 * Deployable artifacts file is a list of DeployableArtifactDetail.
 * The DeployDetails set is prepared in the build artifacts phase. From DeployDetails set we extract the list of DeployableArtifactDetail.
 *
 * Created by yahavi on 25/04/2017.
 */
public class DeployableArtifactsUtils {

    public static void saveDeployableArtifactsToFile(Set<DeployDetails> deployDetails, File toFile) throws IOException {
        List<DeployableArtifactDetail> deployableArtifacts = getDeployableArtifactsPaths(deployDetails);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(toFile, deployableArtifacts);
    }

    public static List<DeployableArtifactDetail> loadDeployableArtifactsFromFile(File fromFile) throws IOException, ClassNotFoundException {
        if (fromFile == null || fromFile.length() == 0) {
            return new ArrayList<DeployableArtifactDetail>();
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(fromFile, new TypeReference<List<DeployableArtifactDetail>>(){});
    }

    private static List<DeployableArtifactDetail> getDeployableArtifactsPaths(Set<DeployDetails> deployDetails) {
        List<DeployableArtifactDetail> deployableArtifacts = new ArrayList<DeployableArtifactDetail>();
        for (DeployDetails artifact : deployDetails) {
            deployableArtifacts.add(new DeployableArtifactDetail(artifact.getFile().getAbsolutePath(), artifact.getArtifactPath(), artifact.getSha1()));
        }
        return deployableArtifacts;
    }
}
