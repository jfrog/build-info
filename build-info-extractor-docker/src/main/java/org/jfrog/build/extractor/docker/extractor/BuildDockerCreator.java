package org.jfrog.build.extractor.docker.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.extractor.ci.BuildInfo;
import org.jfrog.build.extractor.ci.Module;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryManagerBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.artifactory.ArtifactoryManager;
import org.jfrog.build.extractor.clientConfiguration.util.PathsUtils;
import org.jfrog.build.extractor.clientConfiguration.util.spec.UploadSpecHelper;
import org.jfrog.build.extractor.docker.DockerUtils;
import org.jfrog.build.extractor.docker.types.DockerImage;
import org.jfrog.build.extractor.docker.types.DockerLayer;
import org.jfrog.build.extractor.docker.types.DockerLayers;
import org.jfrog.build.extractor.packageManager.PackageManagerExtractor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.createMapper;
import static org.jfrog.build.extractor.packageManager.PackageManagerUtils.createArtifactoryClientConfiguration;

public class BuildDockerCreator extends PackageManagerExtractor {
    private final ArrayListMultimap<String, String> artifactProperties;
    private final ArtifactoryManagerBuilder artifactoryManagerBuilder;
    private final ImageFileType imageFileType;
    private final String sourceRepo;
    private final String imageFile;
    private final Log logger;

    enum ImageFileType {
        KANIKO,
        JIB
    }

    /**
     * @param artifactoryManagerBuilder - Artifactory manager builder.
     * @param sourceRepo                - The repository it'll resolve from.
     * @param imageFileType             - The input imageFile format - JIB or Kaniko
     * @param imageFile                 - Image file to add.
     * @param logger                    - The logger.
     * @param artifactProperties        - Properties to be attached to the docker layers deployed to Artifactory.
     */
    public BuildDockerCreator(ArtifactoryManagerBuilder artifactoryManagerBuilder, String imageFile, ImageFileType imageFileType,
                              ArrayListMultimap<String, String> artifactProperties, String sourceRepo, Log logger) {
        this.artifactoryManagerBuilder = artifactoryManagerBuilder;
        this.artifactProperties = artifactProperties;
        this.sourceRepo = sourceRepo;
        this.imageFileType = imageFileType;
        this.imageFile = imageFile;
        this.logger = logger;
    }

    /**
     * Allow creating build-info for a published docker image in a new Java process.
     *
     * @param ignored ignores input incoming params.
     */
    public static void main(String[] ignored) {
        try {
            ArtifactoryClientConfiguration clientConfiguration = createArtifactoryClientConfiguration();
            // Client builders.
            ArtifactoryManagerBuilder artifactoryManagerBuilder = new ArtifactoryManagerBuilder().setClientConfiguration(clientConfiguration, clientConfiguration.publisher);
            // Load artifact and BuildInfo properties from publisher section in the BuildInfo.properties file.
            ArtifactoryClientConfiguration.DockerHandler dockerHandler = clientConfiguration.dockerHandler;
            String imageFile;
            ImageFileType imageFileType;
            if (StringUtils.isNotBlank(dockerHandler.getKanikoImageFile())) {
                imageFile = dockerHandler.getKanikoImageFile();
                imageFileType = ImageFileType.KANIKO;
            } else if (StringUtils.isNotBlank(dockerHandler.getJibImageFile())) {
                imageFile = dockerHandler.getJibImageFile();
                imageFileType = ImageFileType.JIB;
            } else {
                throw new RuntimeException("kaniko.image.file or jib.image.file property is expected");
            }
            // Init BuildDockerCreate.
            BuildDockerCreator dockerBuildCreate = new BuildDockerCreator(artifactoryManagerBuilder,
                    imageFile,
                    imageFileType,
                    ArrayListMultimap.create(clientConfiguration.publisher.getMatrixParams().asMultimap()),
                    clientConfiguration.publisher.getRepoKey(),
                    clientConfiguration.getLog());

            // Exe build-docker-create & collect build info.
            dockerBuildCreate.executeAndSaveBuildInfo(clientConfiguration);
        } catch (RuntimeException e) {
            ExceptionUtils.printRootCauseStackTrace(e, System.out);
            System.exit(1);
        }
    }

    @Override
    public BuildInfo execute() {
        logger.info("Generating build info for: " + imageFile);
        try {
            List<Module> modules = new ArrayList<>();
            List<ImageFileWithDigest> imageFilesWithDigest = imageFileType == ImageFileType.KANIKO ?
                    getKanikoImageFileWithDigests(imageFile) : getJibImageFilesWithDigests(imageFile);
            if (imageFilesWithDigest.isEmpty()) {
                throw new RuntimeException("No image files found at path '" + imageFile + "'");
            }
            for (ImageFileWithDigest imageFileWithDigest : imageFilesWithDigest) {
                DockerImage image = new DockerImage("", imageFileWithDigest.imageName, imageFileWithDigest.manifestSha256, sourceRepo, artifactoryManagerBuilder, "", "");
                Module module = image.generateBuildInfoModule(logger, DockerUtils.CommandType.Push);
                if (module.getArtifacts() == null || module.getArtifacts().size() == 0) {
                    logger.warn("Could not find docker image: " + imageFileWithDigest.imageName + " in Artifactory.");
                } else {
                    setImageLayersProps(image.getLayers(), artifactProperties, artifactoryManagerBuilder);
                }
                modules.add(module);
                logger.info("Successfully created build info for image: " + imageFileWithDigest.imageName);
            }
            BuildInfo buildInfo = new BuildInfo();
            buildInfo.setModules(modules);
            return buildInfo;
        } catch (Exception e) {
            logger.error(ExceptionUtils.getRootCauseMessage(e), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Update each layer's properties with artifactProperties.
     */
    private void setImageLayersProps(DockerLayers layers, ArrayListMultimap<String, String> artifactProperties, ArtifactoryManagerBuilder artifactoryManagerBuilder) throws IOException {
        if (layers == null) {
            return;
        }
        try (ArtifactoryManager artifactoryManager = artifactoryManagerBuilder.build()) {
            for (DockerLayer layer : layers.getLayers()) {
                artifactoryManager.setProperties(layer.getFullPath(), artifactProperties, true);
            }
        }
    }

    /**
     * Read the file which contains the following format on each line: 'IMAGE-TAG-IN-ARTIFACTORY'@sha256'SHA256-OF-THE-IMAGE-MANIFEST'.
     *
     * @param kanikoImageFile - Kaniko image-file path.
     * @return the image file with names and digests extracted.
     * @throws IOException if an issue occurs reading the file.
     */
    private List<ImageFileWithDigest> getKanikoImageFileWithDigests(String kanikoImageFile) throws IOException {
        List<String> dataLines = Files.readAllLines(Paths.get(kanikoImageFile));
        if (dataLines.isEmpty()) {
            throw new IOException("Couldn't read image file \"" + kanikoImageFile + "\".");
        }
        final List<ImageFileWithDigest> imageFileWithDigests =
                dataLines.stream().map(this::getImageFileWithDigest).filter(Objects::nonNull).collect(Collectors.toList());
        if (imageFileWithDigests.size() != dataLines.size()) {
            throw new RuntimeException("missing image-tag/sha256 in file: \"" + kanikoImageFile + "\"");
        }
        return imageFileWithDigests;
    }

    /**
     * Read the file which contains the following format:
     * {
     * "image":"IMAGE-NAME-IN-ARTIFACTORY",
     * "tags":["IMAGE-TAGS-IN-ARTIFACTORY"]
     * "imageId":"sha256:SHA256-OF-THE-IMAGE-MANIFEST",
     * "imageDigest":"sha256:SHA256-OF-THE-IMAGE",
     * }
     *
     * @param jibImageFiles - Pattern matches JIB's image JSON files. For example: "*target/jib-image.json".
     * @return the image file with names and digests extracted.
     */
    private List<ImageFileWithDigest> getJibImageFilesWithDigests(String jibImageFiles) {
        ObjectMapper mapper = createMapper();
        String baseDir = UploadSpecHelper.getWildcardBaseDir(new File(""), jibImageFiles);
        String newPattern = UploadSpecHelper.prepareWildcardPattern(new File(""), jibImageFiles, baseDir);
        String regexPath = PathsUtils.pathToRegExp(newPattern);
        List<ImageFileWithDigest> imageFilesWithDigests = new ArrayList<>();
        Path baseDirPath = Paths.get(baseDir);
        try (Stream<Path> files = Files.find(baseDirPath,
                Integer.MAX_VALUE,
                (path, basicFileAttributes) -> baseDirPath.relativize(path).toString().matches(regexPath))) {
            files.forEach(jibImageFile -> {
                JsonNode jsonNode;
                try {
                    jsonNode = mapper.readTree(jibImageFile.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("Couldn't read image file \"" + jibImageFiles + "\".");
                }
                JsonNode imageName = jsonNode.get("image");
                if (imageName == null || imageName.isNull()) {
                    throw new RuntimeException("Missing \"image\" in file: \"" + jibImageFiles + "\"");
                }
                JsonNode imageDigest = jsonNode.get("imageDigest");
                if (imageDigest == null || imageDigest.isNull()) {
                    throw new RuntimeException("Missing \"imageDigest\" in file: \"" + jibImageFiles + "\"");
                }
                imageFilesWithDigests.add(new ImageFileWithDigest(imageName.asText(), imageDigest.asText()));
            });
        } catch (IOException e) {
            throw new RuntimeException("Couldn't find JIB image files using the following pattern: '" + jibImageFiles + "'", e);
        }
        return imageFilesWithDigests;
    }

    /**
     * Read the file which contains the following format: 'IMAGE-TAG-IN-ARTIFACTORY'@sha256'SHA256-OF-THE-IMAGE-MANIFEST'.
     *
     * @param data the image file data.
     * @return the image file with name and digest extracted.
     */
    private ImageFileWithDigest getImageFileWithDigest(String data) {
        String[] splitData = data.split("@");
        if (splitData.length != 2) {
            throw new RuntimeException("unexpected file format \"" + data + "\". The file should include one line in the following format: image-tag@sha256");
        }
        ImageFileWithDigest imageFileWithDigest = new ImageFileWithDigest(splitData[0], splitData[1]);
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
