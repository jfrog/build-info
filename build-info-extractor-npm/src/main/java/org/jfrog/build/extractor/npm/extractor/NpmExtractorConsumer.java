package org.jfrog.build.extractor.npm.extractor;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryDependenciesClient;
import org.jfrog.build.extractor.npm.types.NpmPackageInfo;
import org.jfrog.build.extractor.producerConsumer.ConsumerRunnableBase;
import org.jfrog.build.extractor.producerConsumer.ProducerConsumerExecutor;
import org.jfrog.build.api.producerConsumer.ProducerConsumerItem;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Consumes PackageInfos and fills the dependencies map with sha1 and md5.
 *
 * Created by Yahav Itzhak on 25 Nov 2018.
 */
public class NpmExtractorConsumer extends ConsumerRunnableBase {
    private static final String NPM_AQL_FORMAT =
            "items.find({" +
                    "\"@npm.name\": \"%s\"," +
                    "\"@npm.version\": \"%s\"" +
                    "}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")";
    private ArtifactoryDependenciesClient client;
    private Map<String, Dependency> dependencies;
    private ProducerConsumerExecutor executor;
    private Set<NpmPackageInfo> badPackages;
    private Log log;

    NpmExtractorConsumer(ArtifactoryDependenciesClient client, Map<String, Dependency> dependencies, Set<NpmPackageInfo> badPackages) {
        this.client = client;
        this.dependencies = dependencies;
        this.badPackages = badPackages;
    }

    @Override
    public void consumerRun() {
        while (!Thread.interrupted()) {
            try {
                ProducerConsumerItem item = executor.take();
                if (item == executor.TERMINATE) {
                    // If reached the TERMINATE NpmPackageInfo, return it to the queue and exit
                    executor.put(item);
                    break;
                }
                // Perform artifact deploy
                NpmPackageInfo npmPackageInfo = (NpmPackageInfo) item;
                if (!appendDependency(npmPackageInfo)) {
                    badPackages.add(npmPackageInfo);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private Dependency createDependency(NpmPackageInfo npmPackageInfo) {
        String aql = String.format(NPM_AQL_FORMAT, npmPackageInfo.getName(), npmPackageInfo.getVersion());
        AqlSearchResult searchResult;
        try {
            searchResult = client.searchArtifactsByAql(aql);
            if (searchResult.getResults().isEmpty()) {
                return null;
            }
            DependencyBuilder builder = new DependencyBuilder();
            AqlSearchResult.SearchEntry searchEntry = searchResult.getResults().get(0);
            return builder.id(searchEntry.getName())
                    .addScope(npmPackageInfo.getScope())
                    .md5(searchEntry.getActualMd5())
                    .sha1(searchEntry.getActualSha1())
                    .build();
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e), e);
            return null;
        }
    }

    private boolean appendDependency(NpmPackageInfo npmPackageInfo) {
        String id = npmPackageInfo.getName() + ":" + npmPackageInfo.getVersion();
        if (!dependencies.containsKey(id)) {
            Dependency dependency = createDependency(npmPackageInfo);
            if (dependency == null) {
                return false;
            }
            dependencies.put(id, dependency);
        } else {
            dependencies.get(id).getScopes().add(npmPackageInfo.getScope());
        }
        return true;
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
