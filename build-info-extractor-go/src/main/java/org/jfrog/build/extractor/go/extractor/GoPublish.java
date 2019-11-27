package org.jfrog.build.extractor.go.extractor;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.ArtifactoryUploadResponse;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;

import java.io.File;
import java.io.IOException;
import java.lang.InterruptedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings({"unused", "WeakerAccess"})
public class GoPublish extends GoCommand {

    private static final String LOCAL_ZIP_FILENAME = "package.zip";
    private static final String LOCAL_INFO_FILENAME = "package.info";
    private static final String GO_VERSION_PREFIX = "v";
    private static final String PKG_ZIP_FILE_EXTENSION = "zip";
    private static final String PKG_MOD_FILE_EXTENSION = "mod";
    private static final String PKG_INFO_FILE_EXTENSION = "info";

    private ArrayListMultimap<String, String> properties;
    private List<Artifact> artifactList = Lists.newArrayList();
    private String deploymentRepo;
    private String version;

    /**
     * Publish go package.
     *
     * @param clientBuilder - Client builder for deployment.
     * @param properties    - Properties to set on each deployed artifact (Build name, Build number, etc...).
     * @param repo          - Artifactory's repository for deployment.
     * @param path          - Path to directory contains go.mod.
     * @param version       - The package's version.
     * @param logger        - The logger.
     */
    public GoPublish(ArtifactoryBuildInfoClientBuilder clientBuilder, ArrayListMultimap<String, String> properties, String repo, Path path, String version, Log logger) throws  IOException {
        super(clientBuilder, path, logger);
        this.properties = properties;
        this.deploymentRepo = repo;
        this.version = GO_VERSION_PREFIX + version;
    }

    public Build execute() {
        try (ArtifactoryBuildInfoClient dependenciesClient = (ArtifactoryBuildInfoClient) clientBuilder.build()) {
            client = dependenciesClient;
            preparePrerequisites(deploymentRepo, client);
            publishPkg();
            return createBuild(artifactList, null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /*
     * Go pkg contains 3 files:
     *     1. zip file of source files.
     *     2. go.mod file.
     *     3. go.info file.
     * */
    private void publishPkg() throws IOException, NoSuchAlgorithmException, InterruptedException {
        createAndDeployZip();
        deployGoMod();
        createAndDeployInfo();
    }

    private void createAndDeployZip() throws IOException, NoSuchAlgorithmException, InterruptedException { //, ArchiveException {
        String zipLocalPath = path.toString() + "/" + LOCAL_ZIP_FILENAME;
        File zipFile = zipProjectDir(zipLocalPath); // TODO: we need to call ARTIFACTORY code to filter the project zip
        Artifact deployedPackage = deploy(zipFile, PKG_ZIP_FILE_EXTENSION);
        artifactList.add(deployedPackage);
        zipFile.delete();
    }

    private void deployGoMod() throws IOException, NoSuchAlgorithmException {
        String modLocalPath = getModFilePath();
        Artifact deployedMod = deploy(new File(modLocalPath), PKG_MOD_FILE_EXTENSION);
        artifactList.add(deployedMod);
    }

    private void createAndDeployInfo() throws IOException, NoSuchAlgorithmException {
        String infoLocalPath = path.toString() + "/" + LOCAL_INFO_FILENAME;
        File infoFile = writeInfoFile(infoLocalPath);
        Artifact deployedInfo = deploy(infoFile, PKG_INFO_FILE_EXTENSION);
        artifactList.add(deployedInfo);
        infoFile.delete();
    }

    private File zipProjectDir(String localZipPath) throws IOException {
        File zipFile = new File(localZipPath);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            List<Path> pathsList = Files.walk(path)
                    .filter(p -> !Files.isDirectory(p) && !p.toFile().getName().equals(LOCAL_ZIP_FILENAME))
                    .collect(Collectors.toList());
            for (Path filePath : pathsList) {
                ZipEntry zipEntry = new ZipEntry(path.relativize(filePath).toString());
                zos.putNextEntry(zipEntry);
                Files.copy(filePath, zos);
                zos.closeEntry();
            }
        }

        return zipFile;
    }

    /*
     * pkg info is a json file contains:
     *      1. The package's version.
     *      2. The package creation timestamp.
     */
    private File writeInfoFile(String localInfoPath) throws IOException{
        File infoFile = new File(localInfoPath);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> infoMap = new HashMap();
        Date date = new Date();
        Instant instant = date.toInstant();

        infoMap.put("Version", version);
        infoMap.put("Time", instant.toString());

        mapper.writeValue(infoFile, infoMap);
        return infoFile;
    }

    /* Deploy pkg file and add it as an buildInfo's artifact */
    private Artifact deploy(File deployedFile, String extension) throws IOException, NoSuchAlgorithmException {
        String artifactName = version + "." + extension;
        Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(deployedFile, MD5, SHA1);
        DeployDetails deployDetails = new DeployDetails.Builder()
                .file(deployedFile)
                .targetRepository(deploymentRepo)
                .addProperties(properties)
                .artifactPath(moduleName + "/@v/" + artifactName)
                .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                .build();

        ArtifactoryUploadResponse response = ((ArtifactoryBuildInfoClient) client).deployArtifact(deployDetails);

        Artifact deployedArtifact =  new ArtifactBuilder(moduleName + ":" + artifactName)
                .md5(response.getChecksums().getMd5())
                .sha1(response.getChecksums().getSha1())
                .build();

        return deployedArtifact;
    }
}
