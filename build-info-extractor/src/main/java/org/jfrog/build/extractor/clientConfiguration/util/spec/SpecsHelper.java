package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryBuildInfoClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.util.DependenciesDownloaderHelper;
import org.jfrog.build.extractor.clientConfiguration.util.EditPropertiesHelper;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SearchBasedSpecValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SpecsValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.UploadSpecValidator;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jfrog.build.client.PreemptiveHttpClientBuilder.CONNECTION_POOL_SIZE;

/**
 * Created by diman on 24/08/2016.
 */
public class SpecsHelper {

    private static final int DEFAULT_NUMBER_OF_THREADS = 3; // default number of threads for file spec uploads
    private final Log log;

    public SpecsHelper(Log log) {
        this.log = log;
    }

    /**
     * Upload artifacts according to a given spec, return a list describing the deployed items.
     * Retains compatibility with other plugins using file specs
     * with default number of concurrent upload threads
     *
     * @param uploadSpec      The required spec represented as String
     * @param workspace       File object that represents the workspace
     * @param buildProperties Upload properties
     * @param clientBuilder   ArtifactoryBuildInfoClientBuilder which will build the buildInfoClients to perform the actual upload
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     */
    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Map<String, String> buildProperties,
                                                ArtifactoryBuildInfoClientBuilder clientBuilder) throws Exception {
        return uploadArtifactsBySpec(uploadSpec, DEFAULT_NUMBER_OF_THREADS, workspace, createMultiMap(buildProperties), clientBuilder);
    }

    /**
     * Upload artifacts according to a given spec, return a list describing the deployed items.
     *
     * @param uploadSpec      The required spec represented as String
     * @param workspace       File object that represents the workspace
     * @param buildProperties Upload properties
     * @param clientBuilder   ArtifactoryBuildInfoClientBuilder which will build the buildInfoClients to perform the actual upload
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     */
    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Multimap<String, String> buildProperties,
                                                ArtifactoryBuildInfoClientBuilder clientBuilder) throws Exception {
        return uploadArtifactsBySpec(uploadSpec, DEFAULT_NUMBER_OF_THREADS, workspace, buildProperties, clientBuilder);
    }


    /**
     * Upload artifacts according to a given spec, return a list describing the deployed items.
     *
     * @param uploadSpec      The required spec represented as String
     * @param numberOfThreads Number of concurrent threads to use for handling uploads
     * @param workspace       File object that represents the workspace
     * @param buildProperties Upload properties
     * @param clientBuilder   ArtifactoryBuildInfoClientBuilder which will build the buildInfoClients per the number of passed threads number to perform the actual upload
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     */
    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, int numberOfThreads, File workspace,
                                                Multimap<String, String> buildProperties,
                                                ArtifactoryBuildInfoClientBuilder clientBuilder) throws Exception {
        Spec spec = this.getSpecFromString(uploadSpec, new UploadSpecValidator());

        try (ArtifactoryBuildInfoClient client = clientBuilder.build()) {
            // Create producer Runnable
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new SpecDeploymentProducer(spec, workspace, buildProperties)};
            // Create consumer Runnables
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[numberOfThreads];
            for (int i = 0; i < numberOfThreads; i++) {
                consumerRunnables[i] = new SpecDeploymentConsumer(client);
            }
            // Create the deployment executor
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(log, producerRunnable, consumerRunnables, CONNECTION_POOL_SIZE);

            deploymentExecutor.start();
            Set<DeployDetails> deployedArtifacts = ((SpecDeploymentProducer) producerRunnable[0]).getDeployedArtifacts();
            return convertDeployDetailsToArtifacts(deployedArtifacts);
        }
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
     * @param spec            the spec to use for download.
     * @param client          the client to use for download.
     * @param targetDirectory the target directory in case of relative path in the spec
     * @return A list of the downloaded dependencies.
     * @throws IOException in case of IOException
     */
    public List<Dependency> downloadArtifactsBySpec(String spec, ArtifactoryDependenciesClient client, String targetDirectory) throws IOException {
        DependenciesDownloaderHelper helper = new DependenciesDownloaderHelper(client, targetDirectory, log);
        return helper.downloadDependencies(getSpecFromString(spec, new SearchBasedSpecValidator()));
    }

    /**
     * Converts File to Spec object
     *
     * @param specFile the File to convert
     * @return Spec object that represents the provided file
     * @throws IOException in case of IO problem
     */
    public Spec getSpecFromFile(File specFile, SpecsValidator specsValidator) throws IOException {
        return getSpecFromString(FileUtils.readFileToString(specFile, "UTF-8"), specsValidator);
    }

    /**
     * Converts String to Spec object
     *
     * @param specStr the String to convert
     * @return Spec object that represents the string
     * @throws IOException in case of IO problem
     */
    public Spec getSpecFromString(String specStr, SpecsValidator specsValidator) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // When mapping the spec from String to Spec one backslash is being removed, multiplying the backslashes solves this.
        specStr = specStr.replace("\\", "\\\\");
        Spec spec = mapper.readValue(specStr, Spec.class);
        specsValidator.validate(spec, log);
        pathToUnixFormat(spec);
        return spec;
    }

    @SuppressWarnings("unused")
    public boolean editPropertiesBySpec(String spec, ArtifactoryDependenciesClient client,
                                        EditPropertiesHelper.EditPropertiesActionType editType, String props) throws IOException {
        EditPropertiesHelper helper = new EditPropertiesHelper(client, log);
        return helper.editProperties(getSpecFromString(spec, new SearchBasedSpecValidator()), editType, props);
    }

    private void pathToUnixFormat(Spec spec) {
        for (FileSpec fileSpec : spec.getFiles()) {
            // In case of regex double backslashes are separator
            String separator = StringUtils.equalsIgnoreCase(fileSpec.getRegexp(), Boolean.TRUE.toString()) ? "\\\\\\\\" : "\\\\";
            if (fileSpec.getTarget() != null) {
                fileSpec.setTarget(fileSpec.getTarget().replaceAll("\\\\", "/"));
            }
            if (fileSpec.getPattern() != null) {
                fileSpec.setPattern(fileSpec.getPattern().replaceAll(separator, "/"));
            }
            if (!ArrayUtils.isEmpty(fileSpec.getExclusions())) {
                fileSpec.setExclusions(fixExclusionsPathToUnixFormat(fileSpec.getExclusions(), separator));
            } else if (!ArrayUtils.isEmpty(fileSpec.getExcludePatterns())) {
                fileSpec.setExcludePatterns(fixExclusionsPathToUnixFormat(fileSpec.getExcludePatterns(), separator));
            }
        }
    }

    private String[] fixExclusionsPathToUnixFormat(String[] exclusions, String separator) {
        for (int i = 0; i < exclusions.length; i++) {
            String exclusion = exclusions[i];
            exclusions[i] = exclusion.replaceAll(separator, "/");
        }
        return exclusions;
    }

    /**
     * Builds a map representing Spec's properties
     *
     * @param props Spec's properties
     * @return created properties map
     */
    public static ArrayListMultimap<String, String> getPropertiesMap(String props) {
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

    private List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
        List<Artifact> result = new ArrayList<>();
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName());
            ArtifactBuilder artifactBuilder = new ArtifactBuilder(detail.getFile().getName());
            artifactBuilder
                    .md5(detail.getMd5())
                    .sha1(detail.getSha1())
                    .type(ext)
                    .localPath(detail.getFile().getAbsolutePath())
                    .remotePath(detail.getArtifactPath())
                    .build();
            result.add(artifactBuilder.build());
        }
        return result;
    }
}
