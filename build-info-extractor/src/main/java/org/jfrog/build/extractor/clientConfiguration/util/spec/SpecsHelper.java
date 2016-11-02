package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper;
import org.jfrog.build.api.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
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
     * Returns Set of deploy details that represents the given spec
     *
     * @param uploadJson The required spec represented as Spec object
     * @param workspace File object that represents the workspace
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     * @throws NoSuchAlgorithmException Thrown if any of the given algorithms aren't supported
     */
    public Set<DeployDetails> getDeployDetails(Spec uploadJson, File workspace)
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
    public Set<DeployDetails> getDeployDetails(Spec uploadJson, File workspace, ArrayListMultimap<String, String> buildProperties)
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
    public Spec getDownloadUploadSpec(File downloadUploadSpecFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String downloadUploadSpec = FileUtils.readFileToString(downloadUploadSpecFile);
        return mapper.readValue(downloadUploadSpec.replace("\\", "/"), Spec.class);
    }

    /**
     * Converts String to Spec object
     *
     * @param downloadUploadSpec the String to convert
     * @return Spec object that represents the string
     * @throws IOException in case of IO problem
     */
    public Spec getDownloadUploadSpec(String downloadUploadSpec) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(downloadUploadSpec.replace("\\", "/"), Spec.class);
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
                                                               ArrayListMultimap<String, String> buildProperties)
            throws IOException, NoSuchAlgorithmException {
        Set<DeployDetails> result = Sets.newHashSet();
        String targetPath = fileEntry.getKey();
        File artifactFile = fileEntry.getValue();
        String path = PublishedItemsHelper.wildcardCalculateTargetPath(targetPath, artifactFile);
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

        result = PublishedItemsHelper.wildCardBuildPublishingData(
                        workspace, pattern, targetPath, isFlat, isRecursive, isRegexp);
        if (result != null) {
            log.info(String.format("For pattern: %s %d artifacts were found.", pattern, result.size()));
        } else {
            log.info(String.format("For pattern: %s no artifacts were found", pattern));
        }
        return result;
    }

    private ArrayListMultimap<String, String> getPropertiesMap(String props) {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create();
        if (props == null) {
            return properties;
        }
        for (String prop : props.trim().split(";")) {
            String key = StringUtils.substringBefore(prop, "=");
            String values = StringUtils.substringAfter(prop, "=");
            for (String value : values.split(",")) {
                properties.put(key, value);
            }
        }
        return properties;
    }

    private String getLocalPath(String path) {
        return StringUtils.substringAfter(path, "/");
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
}
