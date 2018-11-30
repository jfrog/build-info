package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientBuilderBase;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryDependenciesClientBuilder;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.extractor.producerConsumer.ProducerRunnableBase;

import javax.swing.tree.DefaultMutableTreeNode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
@SuppressWarnings("unused")
public class NpmBuildInfoExtractor implements BuildInfoExtractor<NpmProject, List<Dependency>> {

    private ArtifactoryDependenciesClientBuilder clientBuilder;
    private Map<String, Dependency> dependencies = new ConcurrentHashMap<>();
    private Set<PackageInfo> badPackages = Collections.synchronizedSet(new HashSet<>());
    private Log logger;

    @SuppressWarnings({"WeakerAccess"})
    public NpmBuildInfoExtractor(ArtifactoryClientBuilderBase clientBuilder, Log logger) {
        this.clientBuilder = (ArtifactoryDependenciesClientBuilder) clientBuilder;
        this.logger = logger;
    }

    /**
     * Extract a list of dependencies using the results of 'npm ls' commands.
     *
     * @param npmProject - The npm project contains the results of the 'npm ls' commands.
     * @return list of dependencies
     */
    @Override
    public List<Dependency> extract(NpmProject npmProject) {
        npmProject.getDependencies().forEach(this::populateDependencies);
        if (!badPackages.isEmpty()) {
            logger.info((Arrays.toString(badPackages.toArray())));
            logger.info("The npm dependencies above could not be found in Artifactory and therefore are not included in the build-info. " +
                    "Make sure the dependencies are available in Artifactory for this build.");
        }
        return new ArrayList<>(dependencies.values());
    }


    private void populateDependencies(Pair<NpmScope, JsonNode> nodeWithScope) {
        DefaultMutableTreeNode rootNode = NpmDependencyTree.createDependenciesTree(nodeWithScope.getKey(), nodeWithScope.getValue());
        try (ArtifactoryDependenciesClient client1 = clientBuilder.build();
             ArtifactoryDependenciesClient client2 = clientBuilder.build();
             ArtifactoryDependenciesClient client3 = clientBuilder.build()
        ) {
            // Create producer Runnable
            ProducerRunnableBase[] producerRunnable = new ProducerRunnableBase[]{new NpmExtractProducer(rootNode)};
            // Create consumer Runnables
            ConsumerRunnableBase[] consumerRunnables = new ConsumerRunnableBase[]{
                    new NpmExtractorConsumer(client1, dependencies, badPackages),
                    new NpmExtractorConsumer(client2, dependencies, badPackages),
                    new NpmExtractorConsumer(client3, dependencies, badPackages)
            };
            // Create the deployment executor
            ProducerConsumerExecutor deploymentExecutor = new ProducerConsumerExecutor(logger, producerRunnable, consumerRunnables, 10);
            deploymentExecutor.start();
        } catch (Exception e) {
            logger.error(ExceptionUtils.getStackTrace(e), e);
        }
    }
}
