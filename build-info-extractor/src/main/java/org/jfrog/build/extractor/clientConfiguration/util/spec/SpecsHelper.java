package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.DownloadSpecValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SpecsValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.UploadSpecValidator;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by diman on 24/08/2016.
 */
public class SpecsHelper {

    private static final String SHA1 = "SHA1";
    private static final String MD5 = "MD5";
    private final Log log;

    public SpecsHelper(Log log) {
        this.log = log;
    }

    /**
     * Uploads and returns List of artifacts according to a provided by the user upload fileSpec
     *
     * @param uploadSpec The required spec represented as String
     * @param workspace File object that represents the workspace
     * @param buildProperties Upload properties
     * @param client ArtifactoryBuildInfoClient which will do the actual upload
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     */
    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Multimap<String, String> buildProperties,
                                                ArtifactoryBuildInfoClient client)
            throws IOException, NoSuchAlgorithmException {
        Spec spec = this.getDownloadUploadSpec(uploadSpec, new UploadSpecValidator());
        Set<DeployDetails> artifactsToDeploy = getDeployDetails(spec, workspace, buildProperties);
        deploy(client, artifactsToDeploy);
        return convertDeployDetailsToArtifacts(artifactsToDeploy);
    }

    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Map<String, String> buildProperties,
                                                ArtifactoryBuildInfoClient client)
            throws IOException, NoSuchAlgorithmException {
        return uploadArtifactsBySpec(uploadSpec, workspace, createMultiMap(buildProperties), client);
    }

    public static <K, V> Multimap<K, V> createMultiMap(Map<K, V> input) {
        Multimap<K, V> multimap = ArrayListMultimap.create();
        for (Map.Entry<K, V> entry : input.entrySet()) {
            multimap.put(entry.getKey(), entry.getValue());
        }
        return multimap;
    }

    /**
     * Downloads Artifacts by spec and returns a list of the downloaded dependencies.
     * The artifacts will be downloaded using the provided client.
     * In case of relative path the artifacts will be downloaded to the targetDirectory.
     *
     * @param spec the spec to use for download.
     * @param client the client to use for download.
     * @param targetDirectory the target directory in case of relative path in the spec
     * @return A list of the downloaded dependencies.
     * @throws IOException in case of IOException
     */
    public List<Dependency> downloadArtifactsBySpec(String spec, ArtifactoryDependenciesClient client, String targetDirectory) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(client, targetDirectory, log);
        return helper.downloadDependencies(getDownloadUploadSpec(spec, new DownloadSpecValidator()));
    }

    /**
     * Returns Set of deploy details that represents the given spec
     *
     * @param uploadJson The required spec represented as Spec object
     * @param workspace File object that represents the workspace
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     */
    private Set<DeployDetails> getDeployDetails(Spec uploadJson, File workspace)
            throws IOException, NoSuchAlgorithmException {
        return getDeployDetails(uploadJson, workspace, null);
    }

    /**
     * Returns Set of deploy details that represents the given spec
     *
     * @param uploadJson The required spec represented as Spec object
     * @param workspace File object that represents the workspace
     * @param buildProperties properties to add to all the files
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     */
    private Set<DeployDetails> getDeployDetails(Spec uploadJson, File workspace, Multimap<String, String> buildProperties)
            throws IOException, NoSuchAlgorithmException {
        log.debug("Getting deploy details from spec.");
        Set<DeployDetails> artifactsToDeploy = Sets.newHashSet();
        for (FileSpec uploadFile : uploadJson.getFiles()) {
            validateUploadSpec(uploadFile);
            log.debug(String.format("Getting deploy details from the following json: \n %s ", uploadFile.toString()));
            Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace ,uploadFile);
            for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
                artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry, uploadFile, buildProperties));
            }
        }
        return artifactsToDeploy;
    }

    /**
     * Converts File to Spec object
     *
     * @param downloadUploadSpecFile the File to convert
     * @return Spec object that represents the provided file
     * @throws IOException in case of IO problem
     */
    public Spec getDownloadUploadSpec(File downloadUploadSpecFile, SpecsValidator specsValidator) throws IOException {
        return getDownloadUploadSpec(FileUtils.readFileToString(downloadUploadSpecFile), specsValidator);
    }

    /**
     * Converts String to Spec object
     *
     * @param downloadUploadSpec the String to convert
     * @return Spec object that represents the string
     * @throws IOException in case of IO problem
     */
    public Spec getDownloadUploadSpec(String downloadUploadSpec, SpecsValidator specsValidator) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // When mapping the spec from String to Spec one backslash is being removed, multiplying the backslashes solves this.
        downloadUploadSpec = downloadUploadSpec.replace("\\", "\\\\");
        Spec spec = mapper.readValue(downloadUploadSpec, Spec.class);
        specsValidator.validate(spec);
        pathToUnixFormat(spec);
        return spec;
    }

    private void pathToUnixFormat(Spec spec) {
        for (FileSpec fileSpec : spec.getFiles()) {
            if (fileSpec.getTarget() != null) {
                fileSpec.setTarget(fileSpec.getTarget().replaceAll("\\\\", "/"));
            }
            if (fileSpec.getPattern() != null) {
                if (!StringUtils.equalsIgnoreCase(fileSpec.getRegexp(), Boolean.TRUE.toString())) {
                    fileSpec.setPattern(fileSpec.getPattern().replaceAll("\\\\", "/"));
                } else {
                    // In case of regex double backslashes are separator
                    fileSpec.setPattern(fileSpec.getPattern().replaceAll("\\\\\\\\", "/"));
                }
            }
        }
    }

    /**
     * Creates set of DeployDetails from provided map of String->File entries, FileSpec and Properties
     *
     * @param fileEntry the FileSpec that contains the needed params.
     * @param uploadFile a map of String->File entries that will be returned as a set of DeployDetails.
     * @param buildProperties a map of properties to add to all the DeployDetails objects.
     * @return Set of DeployDetails that represents the provided map of fileEntries aggregated with the ptops and the
     *         target provided in the uploadFile and buildProperties.
     * @throws IOException in case of IO problem.
     * @throws NoSuchAlgorithmException if appropriate checksum algorithm was not found.
     */
    private Set<DeployDetails> buildDeployDetailsFromFileEntry(Map.Entry<String, File> fileEntry, FileSpec uploadFile,
                                                               Multimap<String, String> buildProperties)
            throws IOException, NoSuchAlgorithmException {
        Set<DeployDetails> result = Sets.newHashSet();
        String targetPath = fileEntry.getKey();
        File artifactFile = fileEntry.getValue();
        String path = UploadSpecHelper.wildcardCalculateTargetPath(targetPath, artifactFile);
        path = StringUtils.replace(path, "//", "/");

        // calculate the sha1 checksum and add it to the deploy artifactsToDeploy
        Map<String, String> checksums;
        try {
            checksums = FileChecksumCalculator.calculateChecksums(artifactFile, SHA1, MD5);
        } catch (NoSuchAlgorithmException e) {
            throw new NoSuchAlgorithmException(
                    String.format("Could not find checksum algorithm for %s or %s.", SHA1, MD5), e);
        }
        DeployDetails.Builder builder = new DeployDetails.Builder()
                .file(artifactFile)
                .artifactPath(path)
                .targetRepository(getRepositoryKey(uploadFile.getTarget()))
                .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                .explode(BooleanUtils.toBoolean(uploadFile.getExplode()))
                .addProperties(getPropertiesMap(uploadFile.getProps()));
        if (buildProperties != null && !buildProperties.isEmpty()) {
            builder.addProperties(buildProperties);
        }
        result.add(builder.build());

        return result;
    }

    private Multimap<String, File> buildTargetPathToFiles(File workspace, FileSpec uploadFile) throws IOException {
        // The default value is true so it should be true in any case the string not matches "false"
        boolean isFlat = !"false".equalsIgnoreCase(uploadFile.getFlat());
        boolean isRecursive = !"false".equalsIgnoreCase(uploadFile.getRecursive());
        boolean isRegexp = BooleanUtils.toBoolean(uploadFile.getRegexp());
        String pattern = uploadFile.getPattern();
        String targetPath = getLocalPath(uploadFile.getTarget());
        Multimap<String, File> result;

        result = UploadSpecHelper.buildPublishingData(
                workspace, pattern, targetPath, isFlat, isRecursive, isRegexp);
        if (result != null) {
            log.info(String.format("For pattern: %s %d artifacts were found.", pattern, result.size()));
        } else {
            log.info(String.format("For pattern: %s no artifacts were found", pattern));
        }
        return result;
    }

    private ArrayListMultimap<String, String> getPropertiesMap(String props) {
        ArrayListMultimap<String, String> propertiesMap = ArrayListMultimap.create();
        fillPropertiesMap(props, propertiesMap);
        return propertiesMap;
    }

    public static void fillPropertiesMap(String props, ArrayListMultimap<String, String> propertiesMap) {
        if (StringUtils.isBlank(props)) {
            return;
        }
        for (String prop : props.trim().split(";")) {
            String key = StringUtils.substringBefore(prop, "=");
            String[] values = StringUtils.substringAfter(prop, "=").split(",");
            propertiesMap.putAll(key, Arrays.asList(values));
        }
    }

    private String getLocalPath(String path) {
        path = StringUtils.substringAfter(path, "/");
        // When the path is the root of the repo substringAfter will return empty string. in such case slash need to be added
        if ("".equals(path)) {
            return "/";
        }
        return path;
    }

    private String getRepositoryKey(String path) {
        return StringUtils.substringBefore(path, "/");
    }

    private void validateUploadSpec(FileSpec uploadFile) {
        if (StringUtils.isEmpty(uploadFile.getTarget())) {
            throw new IllegalArgumentException("The argument 'target' is missing from the upload spec.");
        }
        if (StringUtils.isEmpty(uploadFile.getPattern())) {
            throw new IllegalArgumentException("The argument 'pattern' is missing from the upload spec.");
        }
    }

    private List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
        List<Artifact> result = Lists.newArrayList();
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName());
            Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                    .sha1(detail.getSha1()).type(ext).build();
            result.add(artifact);
        }
        return result;
    }

    private void deploy(ArtifactoryBuildInfoClient client, Set<DeployDetails> artifactsToDeploy) throws IOException {
        for (DeployDetails deployDetail : artifactsToDeploy) {
            client.deployArtifact(deployDetail);
        }
    }
}
