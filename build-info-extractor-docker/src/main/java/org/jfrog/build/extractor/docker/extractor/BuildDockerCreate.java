package org.jfrog.build.extractor.docker.extractor;

import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.docker.DockerUtils;
import org.jfrog.build.extractor.docker.types.DockerImage;
import org.jfrog.build.extractor.docker.types.DockerLayer;
import org.jfrog.build.extractor.docker.types.DockerLayers;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class BuildDockerCreate extends PackageManagerExtractor {
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private final Log logger;
    private final String kanikoImageFile;
    private final String targetRepository;
    private final List<Module> modulesList = new ArrayList<>();
    private final ArrayListMultimap<String, String> artifactProperties;


    /**
     * @param artifactoryManagerBuilder - Artifactory manager builder.
     * @param targetRepository          - The repository it'll deploy to.
     * @param kanikoImageFile           - Image file to add.
     * @param logger                    - The logger.
     * @param artifactProperties        - Properties to be attached to the docker layers deployed to Artifactory.
     */
    public BuildDockerCreate(ArtifactoryManagerBuilder artifactoryManagerBuilder,
                      String kanikoImageFile, ArrayListMultimap<String, String> artifactProperties, String targetRepository, Log logger) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.targetRepository = targetRepository;
        this.logger = logger;
        this.kanikoImageFile = kanikoImageFile;
        this.artifactProperties = artifactProperties;
    }

    /**
     * Allow running docker push using a new Java process.
     *
     * @param ignored ignores input incoming params.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            // Client builders.
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.publisher);
            // Load artifact and BuildInfo properties from publisher section in the BuildInfo.properties file.
            ArtifactoryClientConfiguration.KanikoHandler kanikoHandler = clientConfiguration.kanikoHandler;
            // Init BuildDockerCreate.
            BuildDockerCreate dockerBuildCreate = new BuildDockerCreate(artifactoryManagerBuilder,
                    kanikoHandler.getImageFile(),
                    ArrayListMultimap.create(clientConfiguration.publisher.getMatrixParams().asMultimap()),
                    clientConfiguration.publisher.getRepoKey(),
                    clientConfiguration.getLog());

            // Exe docker push & collect build info.
            dockerBuildCreate.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public Build execute() {
        logger.info("Getting build info for: " + kanikoImageFile);
        try {
            Build build = new Build();
            for (ImageFileWithDigest imageFileWithDigest : getImageFileWithDigests(kanikoImageFile)) {
                DockerImage image = new DockerImage(imageFileWithDigest.manifestSha256, imageFileWithDigest.imageName, targetRepository, artifactoryManagerBuilder, "", "");
                Module module = image.generateBuildInfoModule(logger, DockerUtils.CommandType.Push);
                if (module.getArtifacts() == null || module.getArtifacts().size() == 0) {
                    logger.warn("Could not find docker image: " + imageFileWithDigest.imageName + " in Artifactory.");
                } else {
                    setImageLayersProps(image.getLayers(), artifactProperties, artifactoryManagerBuilder);
                }
                modulesList.add(module);
                logger.info("Successfully created build info for image: " + imageFileWithDigest.imageName);
            }
            build.setModules(modulesList);
            return build;
        } catch (IOException | InterruptedException e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update each layer's properties with artifactProperties.
     */
    private void setImageLayersProps(DockerLayers layers, ArrayListMultimap<String, String> artifactProperties, ArtifactoryManagerBuilder artifactoryManagerBuilder) throws IOException {
        if (layers == null){
            return;
        }
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            for (DockerLayer layer : layers.getLayers()) {
                artifactoryManager.setProperties(layer.getFullPath(), artifactProperties, false);
            }
        }
    }

    /**
     * Read the file which contains the following format on each line: 'IMAGE-TAG-IN-ARTIFACTORY'@sha256'SHA256-OF-THE-IMAGE-MANIFEST'.
     *
     * @param filePath the image file path.
     * @return the image file with names and digests extracted.
     * @throws IOException if an issue occurs reading the file.
     */
    private List<ImageFileWithDigest> getImageFileWithDigests(String filePath) throws IOException {
        List<String> dataLines = Files.readAllLines(Paths.get(filePath));
        if (dataLines.isEmpty()) {
            throw new IOException("empty image file \"" + filePath + "\".");
        }
        final List<ImageFileWithDigest> imageFileWithDigests =
                dataLines.stream().map(this::getImageFileWithDigest).filter(Objects::nonNull).collect(Collectors.toList());
        if (imageFileWithDigests.size() != dataLines.size()) {
            throw new RuntimeException("missing image-tag/sha256 in file: \"" + filePath + "\"");
        }
        return imageFileWithDigests;
    }

    /**
     * Read the file which contains the following format: 'IMAGE-TAG-IN-ARTIFACTORY'@sha256'SHA256-OF-THE-IMAGE-MANIFEST'.
     *
     * @param data the image file data.
     * @return the image file with name and digest extracted.
     */
    private ImageFileWithDigest getImageFileWithDigest(String data) {
        String[] splittedData = data.split("@");
        if (splittedData.length != 2) {
            throw new RuntimeException("unexpected file format \"" + data + "\". The file should include one line in the following format: image-tag@sha256");
        }
        ImageFileWithDigest imageFileWithDigest = new ImageFileWithDigest(splittedData[0], splittedData[1]);
        if (imageFileWithDigest.imageName.isEmpty() || imageFileWithDigest.manifestSha256.isEmpty()) {
            return null;
        }
        return imageFileWithDigest;
    }

    private static class ImageFileWithDigest {
        final String imageName;
        final String manifestSha256;

        ImageFileWithDigest(String imageName, String manifestSha256) {
            this.imageName = imageName.trim();
            this.manifestSha256 = manifestSha256.trim();
        }
    }
}
