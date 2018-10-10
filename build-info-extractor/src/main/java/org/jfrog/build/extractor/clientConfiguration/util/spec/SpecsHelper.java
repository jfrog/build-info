package org.jfrog.build.extractor.clientConfiguration.util.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
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
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.DownloadSpecValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.SpecsValidator;
import org.jfrog.build.extractor.clientConfiguration.util.spec.validator.UploadSpecValidator;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by diman on 24/08/2016.
 */
public class SpecsHelper {

    private final Log log;

    public SpecsHelper(Log log) {
        this.log = log;
    }

    /**
     * Upload artifacts according to a given spec, return a list describing the deployed items.
     *
     * @param uploadSpec The required spec represented as String
     * @param workspace File object that represents the workspace
     * @param buildProperties Upload properties
     * @param clientBuilder ArtifactoryBuildInfoClientBuilder which will build the clients to perform the actual upload
     * @return Set of DeployDetails that was calculated from the given params
     * @throws IOException Thrown if any error occurs while reading the file, calculating the
     *                     checksums or in case of any file system exception
     */
    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Multimap<String, String> buildProperties,
                                                ArtifactoryBuildInfoClientBuilder clientBuilder) throws Exception {
        Spec spec = this.getDownloadUploadSpec(uploadSpec, new UploadSpecValidator());

        try (ArtifactoryBuildInfoClient client1 = clientBuilder.build();
             ArtifactoryBuildInfoClient client2 = clientBuilder.build();
             ArtifactoryBuildInfoClient client3 = clientBuilder.build()
        ) {
            // Create producer Runnable
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new SpecDeploymentProducer(spec, workspace, buildProperties)};
            // Create consumer Runnables
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new SpecDeploymentConsumer(client1),
                    new SpecDeploymentConsumer(client2),
                    new SpecDeploymentConsumer(client3)
            };
            // Create the deployment executor
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(log, producerRunnable, consumerRunnables, 10);

            deploymentExecutor.start();
            Set<DeployDetails> deployedArtifacts = ((SpecDeploymentProducer) producerRunnable[0]).getDeployedArtifacts();
            return convertDeployDetailsToArtifacts(deployedArtifacts);
        }
    }

    public List<Artifact> uploadArtifactsBySpec(String uploadSpec, File workspace,
                                                Map<String, String> buildProperties,
                                                ArtifactoryBuildInfoClientBuilder clientBuilder) throws Exception {
        return uploadArtifactsBySpec(uploadSpec, workspace, createMultiMap(buildProperties), clientBuilder);
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
            // In case of regex double backslashes are separator
            String separator = StringUtils.equalsIgnoreCase(fileSpec.getRegexp(), Boolean.TRUE.toString()) ? "\\\\\\\\" : "\\\\";
            if (fileSpec.getTarget() != null) {
                fileSpec.setTarget(fileSpec.getTarget().replaceAll("\\\\", "/"));
            }
            if (fileSpec.getPattern() != null) {
                fileSpec.setPattern(fileSpec.getPattern().replaceAll(separator, "/"));
            }
            if (fileSpec.getExcludePatterns() != null) {
                for (int i = 0 ; i < fileSpec.getExcludePatterns().length ; i ++) {
                    if (StringUtils.isNotBlank(fileSpec.getExcludePattern(i))) {
                        fileSpec.setExcludePattern(fileSpec.getExcludePattern(i).replaceAll(separator, "/"), i);
                    }
                }
            }
        }
    }

    public static String getExcludePatternsLogStr(String[] excludePatterns) {
        return !ArrayUtils.isEmpty(excludePatterns) ? " with exclude patterns: " + Arrays.toString(excludePatterns) : "";
    }

    /**
     * Builds a map representing Spec's properties
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
        List<Artifact> result = Lists.newArrayList();
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName());
            Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                    .sha1(detail.getSha1()).type(ext).build();
            result.add(artifact);
        }
        return result;
    }
}
