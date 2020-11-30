package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Consumes PackageInfos and fills the dependencies map with sha1 and md5.
 * Retrieves sha1 and md5 information from Artifactory by running an AQL.
 *
 * @author Yahav Itzhak
 */
public class NpmExtractorConsumer extends ConsumerRunnableBase {
    private static final String NPM_AQL_FORMAT =
            "items.find({" +
                    "\"@npm.name\": \"%s\"," +
                    "\"@npm.version\": \"%s\"" +
                    "}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")";
    private ArtifactoryDependenciesClient client;
    private Map<String, Dependency> dependencies;
    private Map<String, Dependency> previousBuildDependencies;
    private ProducerConsumerExecutor executor;
    private Set<NpmPackageInfo> badPackages;
    private Log log;

    NpmExtractorConsumer(ArtifactoryDependenciesClient client, Map<String, Dependency> dependencies,
                         Map<String, Dependency> previousBuildDependencies, Set<NpmPackageInfo> badPackages) {
        this.client = client;
        this.dependencies = dependencies;
        this.previousBuildDependencies = previousBuildDependencies;
        this.badPackages = badPackages;
    }

    @Override
    public void consumerRun() {
        while (!Thread.interrupted()) {
            try {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE NpmPackageInfo, return it to the queue and exit.
                    executor.put(item);
                    break;
                }
                NpmPackageInfo npmPackageInfo = (NpmPackageInfo) item;
                // Try to extract sha1 and md5 for 'npmPackageInfo'. If it doesn't exist in Artifactory's cache, add it to the 'badPackages' list.
                if (!appendDependency(npmPackageInfo)) {
                    badPackages.add(npmPackageInfo);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * If package is included in the dependencies map, add the current scope for the dependency.
     * Otherwise, retrieve sha1 and md5 from Artifactory and add the dependency to the dependencies map.
     *
     * @param npmPackageInfo - The npm package information.
     * @return True if the package is legal. False in case of an error such as an absence in Artifactory's cache.
     */
    private boolean appendDependency(NpmPackageInfo npmPackageInfo) {
        String id = npmPackageInfo.getName() + ":" + npmPackageInfo.getVersion();
        Dependency dependency = dependencies.get(id);
        if (dependency == null) {
            dependency = createDependency(npmPackageInfo, id);
            if (dependency == null) {
                return false;
            }
            dependencies.put(id, dependency);
        } else {
            dependency.getScopes().add(npmPackageInfo.getScope());
        }
        return true;
    }

    /**
     * Create a Dependency for the provided NpmPackageInfo.
     * If the dependency exists in the previous build's dependencies - take the required info from it.
     * Otherwise - fetch the information from Artifactory.
     *
     * @param npmPackageInfo - The npm package information.
     * @param id             - The id of the dependency to create.
     * @return Dependency populated with {name, scope, version, sha1 and md5} or null in case of {error or absence in Artifactory's case}.
     */
    private Dependency createDependency(NpmPackageInfo npmPackageInfo, String id) {
        Dependency previousDependency = previousBuildDependencies.get(id);
        if (previousDependency != null) {
            return createDependencyFromPreviousBuild(npmPackageInfo, previousDependency);
        }
        return createDependencyFromAqlResult(npmPackageInfo, id);
    }

    /**
     * Create 'Dependency' from name and version of 'npmPackageInfo'. Try to retrieve sha1 and md5 from Artifactory.
     *
     * @param npmPackageInfo - The npm package information.
     * @param id             - The id of the dependency to create.
     * @return Dependency or null in case of an exception, or in case the dependency does not exist in Artifactory.
     */
    private Dependency createDependencyFromAqlResult(NpmPackageInfo npmPackageInfo, String id) {
        String aql = String.format(NPM_AQL_FORMAT, npmPackageInfo.getName(), npmPackageInfo.getVersion());
        AqlSearchResult searchResult;
        try {
            searchResult = client.searchArtifactsByAql(aql);
            if (searchResult.getResults().isEmpty()) {
                return null;
            }
            DependencyBuilder builder = new DependencyBuilder();
            AqlSearchResult.SearchEntry searchEntry = searchResult.getResults().get(0);
            return builder.id(id)
                    .addScope(npmPackageInfo.getScope())
                    .md5(searchEntry.getActualMd5())
                    .sha1(searchEntry.getActualSha1())
                    .build();
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    /**
     * Create a dependency using the information fetched from a previously published build.
     *
     * @param npmPackageInfo     - The npm package information.
     * @param previousDependency - Dependency from previous build.
     * @return Dependency populated with {name, scope, version, sha1 and md5}.
     */
    private Dependency createDependencyFromPreviousBuild(NpmPackageInfo npmPackageInfo, Dependency previousDependency) {
        return new DependencyBuilder().id(previousDependency.getId())
                .sha1(previousDependency.getSha1())
                .md5(previousDependency.getMd5())
                .addScope(npmPackageInfo.getScope())
                .build();
    }

    @Override
    public void setExecutor(ProducerConsumerExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void setLog(Log log) {
        this.log = log;
    }
}
