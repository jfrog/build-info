package build.generic

import com.google.common.collect.*
import org.apache.commons.lang.StringUtils
import org.jfrog.build.api.util.FileChecksumCalculator
import org.jfrog.build.client.DeployDetails
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient
import org.jfrog.build.extractor.clientConfiguration.util.PublishedItemsHelper
import utils.TestConstants

import java.security.NoSuchAlgorithmException

/**
 * @author Lior Hasson  
 */
class GenericArtifactsDeployer {
    private static final String SHA1 = "SHA1"
    private static final String MD5 = "MD5"

    private ArtifactoryBuildInfoClient client
    private BuildInfoLog buildInfoLog = new BuildInfoLog(GenericArtifactsDeployer.class)
    private ConfigObject testConfig
    private def buildProperties
    private Set<DeployDetails> artifactsToDeploy = Sets.newHashSet()

    GenericArtifactsDeployer(ConfigObject testConfig, buildProperties, ArtifactoryBuildInfoClient client) {
        this.testConfig = testConfig
        this.buildProperties = buildProperties
        this.client = client
    }

    Set<DeployDetails> getArtifactsToDeploy() {
        return artifactsToDeploy
    }

    public void deploy() throws IOException, InterruptedException {
        String repositoryKey = buildProperties.get(TestConstants.repoKey)
        String deployPattern = testConfig.generic.deployPattern.join("\n")
        Multimap<String, String> pairs = PublishedItemsHelper.getPublishedItemsPatternPairs(deployPattern) 
        if (pairs.isEmpty()) {
            return
        }

        File workspace = new File(getClass().getResource(testConfig.buildLauncher.projectPath).path)
        Multimap<String, File> targetPathToFilesMap = buildTargetPathToFiles(workspace, pairs)
        for (Map.Entry<String, File> entry : targetPathToFilesMap.entries()) {
            artifactsToDeploy.addAll(buildDeployDetailsFromFileEntry(entry, repositoryKey))
        }

        for (DeployDetails deployDetail : artifactsToDeploy) {
            StringBuilder deploymentPathBuilder = new StringBuilder(buildProperties.get(TestConstants.artifactoryUrl))
            deploymentPathBuilder.append("/").append(repositoryKey)
            if (!deployDetail.getArtifactPath().startsWith("/")) {
                deploymentPathBuilder.append("/")
            }
            deploymentPathBuilder.append(deployDetail.getArtifactPath()) 
            buildInfoLog.debug("Deploying artifact: " + deploymentPathBuilder.toString())
            client.deployArtifact(deployDetail)
        }
    }

    private Set<DeployDetails> buildDeployDetailsFromFileEntry(Map.Entry<String, File> fileEntry, String repositoryKey)
            throws IOException {
        Set<DeployDetails> result = Sets.newHashSet()
        String targetPath = fileEntry.getKey()
        File artifactFile = fileEntry.getValue()
        String path = PublishedItemsHelper.calculateTargetPath(targetPath, artifactFile) 
        path = StringUtils.replace(path, "//", "/")
        
        // calculate the sha1 checksum that is not given by Jenkins and add it to the deploy artifactsToDeploy
        Map<String, String> checksums = Maps.newHashMap()
        try {
            checksums = FileChecksumCalculator.calculateChecksums(artifactFile, SHA1, MD5)
        } catch (NoSuchAlgorithmException e) {
            buildInfoLog.error("Could not find checksum algorithm for " + SHA1 + " or " + MD5)
        }
        DeployDetails.Builder builder = new DeployDetails.Builder()
                .file(artifactFile)
                .artifactPath(path)
                .targetRepository(repositoryKey)
                .md5(checksums.get(MD5)).sha1(checksums.get(SHA1))
                .addProperties(getbuildPropertiesMap()) 
        result.add(builder.build()) 

        return result
    }

    /**
     *
     * @param workspace the base directory of which to calculate the given source ant pattern
     * @param patternPairs
     * @return a Multimap containing the targets as keys and the files as values
     * @throws IOException in case of any file system exception
     */
    private Multimap<String, File> buildTargetPathToFiles(File workspace, Multimap<String, String> patternPairs) throws IOException {
        Multimap<String, File> result = HashMultimap.create() 
        for (Map.Entry<String, String> entry : patternPairs.entries()) {
            String pattern = entry.getKey() 
            String targetPath = entry.getValue() 
            Multimap<String, File> publishingData = PublishedItemsHelper.buildPublishingData(workspace, pattern,
                    targetPath) 
            if (publishingData != null) {
                buildInfoLog.debug("For pattern: " + pattern + " " + publishingData.size() + " artifacts were found") 
                result.putAll(publishingData) 
            } else {
                buildInfoLog.debug("For pattern: " + pattern + " no artifacts were found") 
            }
        }

        return result 
    }

    private ArrayListMultimap<String, String> getbuildPropertiesMap() {
        ArrayListMultimap<String, String> properties = ArrayListMultimap.create()

        properties.put("build.name", buildProperties.get(TestConstants.buildName))
        properties.put("build.number", buildProperties.get(TestConstants.buildNumber))
        properties.put("build.timestamp", buildProperties.get(TestConstants.buildTimestamp))

        addMatrixParams properties

        properties
    }

    private void addMatrixParams(Multimap<String, String> properties) {
        String[] matrixParams = StringUtils.split(testConfig.generic.deploymentProperties, " ")
        if (matrixParams == null) {
            return
        }
        for (String matrixParam : matrixParams) {
            String[] split = StringUtils.split(matrixParam, '=')
            if (split.length == 2) {
                //String value = Util.replaceMacro(split[1], env) 
                //Space is not allowed in property key
                properties.put(split[0].replace(" ", StringUtils.EMPTY), split[1])
            }
        }
    }
}
