package build.generic

import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import org.jfrog.build.api.Agent
import org.jfrog.build.api.Artifact
import org.jfrog.build.api.BuildAgent
import org.jfrog.build.api.BuildRetention
import org.jfrog.build.api.BuildType
import org.jfrog.build.api.Dependency
import org.jfrog.build.api.MatrixParameter
import org.jfrog.build.api.builder.ArtifactBuilder
import org.jfrog.build.api.builder.BuildInfoBuilder
import org.jfrog.build.api.builder.ModuleBuilder
import org.jfrog.build.api.dependency.BuildDependency
import org.jfrog.build.client.DeployDetails
import org.jfrog.build.context.BuildContext
import org.jfrog.build.extractor.BuildInfoExtractorUtils
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient

import java.security.NoSuchAlgorithmException

/**
 * @author Lior Hasson  
 */
class GenericBuildInfoDeployer {
    private BuildInfoLog buildInfoLog = new BuildInfoLog(GenericBuildInfoDeployer.class)
    private ArtifactoryBuildInfoClient client
    private List<BuildDependency> buildDependencies
    private List<Dependency> publishedDependencies
    private Set<DeployDetails> artifactsToDeploy = Sets.newHashSet()

    GenericBuildInfoDeployer(ArtifactoryBuildInfoClient client, List<BuildDependency> buildDependencies,
                             List<Dependency> publishedDependencies, Set<DeployDetails> artifactsToDeploy) {
        this.client = client
        this.buildDependencies = buildDependencies
        this.publishedDependencies = publishedDependencies
        this.artifactsToDeploy = artifactsToDeploy
    }

    public void buildInfo(def buildProperties) {
        ArtifactoryClientConfiguration clientConf = new ArtifactoryClientConfiguration(buildInfoLog)
        clientConf.fillFromProperties(buildProperties)
        BuildContext ctx = new BuildContext(clientConf)

        def build = createBuildInfo(ctx)
        try {
            if (clientConf.publisher.isPublishBuildInfo()) {
                client.sendBuildInfo(build)
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e)
        }
    }

    private def createBuildInfo(BuildContext ctx) {
        def buildName = ctx.clientConf.info.buildName
        BuildInfoBuilder builder = new BuildInfoBuilder(buildName)
                .modules(ctx.getModules())
                .durationMillis(System.currentTimeMillis() - ctx.getBuildStartTime())
                .startedDate(new Date(ctx.getBuildStartTime()))
                .buildAgent(new BuildAgent("Generic", "Generic"))

        // This is here for backwards compatibility.
        builder.type(BuildType.GENERIC)
        ArtifactoryClientConfiguration clientConf = ctx.getClientConf()
        String agentName = clientConf.info.getAgentName()
        String agentVersion = clientConf.info.getAgentVersion()
        if (StringUtils.isNotBlank(agentName) && StringUtils.isNotBlank(agentVersion)) {
            builder.agent(new Agent(agentName, agentVersion))
        }
        String buildAgentName = clientConf.info.getBuildAgentName()
        String buildAgentVersion = clientConf.info.getBuildAgentVersion()
        if (StringUtils.isNotBlank(buildAgentName) && StringUtils.isNotBlank(buildAgentVersion)) {
            builder.buildAgent(new BuildAgent(buildAgentName, buildAgentVersion))
        }
        if (StringUtils.isNotBlank(buildName)) {
            builder.name(buildName)
        }
        String buildNumber = clientConf.info.getBuildNumber()
        if (StringUtils.isNotBlank(buildNumber)) {
            builder.number(buildNumber)
        }
        String buildUrl = clientConf.info.getBuildUrl()
        if (StringUtils.isNotBlank(buildUrl)) {
            builder.url(buildUrl)
        }
        String vcsRevision = clientConf.info.getVcsRevision()
        if (StringUtils.isNotBlank(vcsRevision)) {
            builder.vcsRevision(vcsRevision)
        }
        String vcsUrl = clientConf.info.getVcsUrl()
        if (StringUtils.isNotBlank(vcsUrl)) {
            builder.vcsUrl(vcsUrl)
        }
        String principal = clientConf.info.getPrincipal()
        if (StringUtils.isNotBlank(principal)) {
            builder.principal(principal)
        }
        String parentBuildName = clientConf.info.getParentBuildName()
        if (StringUtils.isNotBlank(parentBuildName)) {
            builder.parentName(parentBuildName)
        }
        String parentBuildNumber = clientConf.info.getParentBuildNumber()
        if (StringUtils.isNotBlank(parentBuildNumber)) {
            builder.parentNumber(parentBuildNumber)
        }

        BuildRetention buildRetention = new BuildRetention(clientConf.info.isDeleteBuildArtifacts())
        if (clientConf.info.getBuildRetentionCount() != null) {
            buildRetention.setCount(clientConf.info.getBuildRetentionCount())
        }
        String buildRetentionMinimumDays = clientConf.info.getBuildRetentionMinimumDate()
        if (StringUtils.isNotBlank(buildRetentionMinimumDays)) {
            int minimumDays = Integer.parseInt(buildRetentionMinimumDays)
            if (minimumDays > -1) {
                Calendar calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_YEAR, -minimumDays)
                buildRetention.setMinimumBuildDate(calendar.getTime())
            }
        }
        String[] notToDelete = clientConf.info.getBuildNumbersNotToDelete()
        for (String notToDel : notToDelete) {
            buildRetention.addBuildNotToBeDiscarded(notToDel)
        }
        builder.buildRetention(buildRetention)

        for (Map.Entry<String, String> runParam : clientConf.info.getRunParameters().entrySet()) {
            MatrixParameter matrixParameter = new MatrixParameter(runParam.getKey(), runParam.getValue())
            builder.addRunParameters(matrixParameter)
        }

        if (clientConf.isIncludeEnvVars()) {
            Properties envProperties = new Properties()
            envProperties.putAll(clientConf.getAllProperties())
            envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties, clientConf.getLog())
            for (Map.Entry<Object, Object> envProp : envProperties.entrySet()) {
                builder.addProperty(envProp.getKey(), envProp.getValue())
            }
        }

        def build = builder.build()

        ModuleBuilder moduleBuilder = createDeployDetailsAndAddToBuildInfo(ctx)
        build.setModules(Lists.newArrayList(moduleBuilder.build()))
        build.setBuildDependencies(buildDependencies)

        build
    }

    private ModuleBuilder createDeployDetailsAndAddToBuildInfo(BuildContext ctx) throws IOException, NoSuchAlgorithmException {
        def artifacts = convertDeployDetailsToArtifacts(artifactsToDeploy)

        ModuleBuilder moduleBuilder = new ModuleBuilder()
                .id(ctx.getClientConf().info.getBuildName()  + ":" + ctx.getClientConf().info.getBuildNumber())
                .artifacts(artifacts)
                .dependencies(publishedDependencies)

        moduleBuilder

    }

    private static List<Artifact> convertDeployDetailsToArtifacts(Set<DeployDetails> details) {
        List<Artifact> result = Lists.newArrayList()
        for (DeployDetails detail : details) {
            String ext = FilenameUtils.getExtension(detail.getFile().getName())
            Artifact artifact = new ArtifactBuilder(detail.getFile().getName()).md5(detail.getMd5())
                    .sha1(detail.getSha1()).type(ext).build()
            result.add(artifact)
        }

        result
    }
}
