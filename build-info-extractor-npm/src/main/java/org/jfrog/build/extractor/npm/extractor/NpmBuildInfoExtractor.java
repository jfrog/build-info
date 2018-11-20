package org.jfrog.build.extractor.npm.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jfrog.build.api.Dependency;
import org.jfrog.build.api.PackageInfo;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.search.AqlSearchResult;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.npm.types.NpmProject;
import org.jfrog.build.extractor.npm.types.NpmScope;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.util.*;

@SuppressWarnings("unused")
public class NpmBuildInfoExtractor implements BuildInfoExtractor<NpmProject, List<Dependency>> {
    private static final String NPM_AQL_FORMAT =
            "items.find({" +
                    "\"@npm.name\": \"%s\"," +
                    "\"@npm.version\": \"%s\"" +
                    "}).include(\"name\", \"repo\", \"path\", \"actual_sha1\", \"actual_md5\")";

    private Map<String, Dependency> dependencies = new HashMap<>();
    private Set<PackageInfo> badPackages = new HashSet<>();
    private NpmProject npmProject;

    @Override
    public List<Dependency> extract(NpmProject npmProject) {
        this.npmProject = npmProject;
        npmProject.getDependencies().forEach(this::populateDependencies);
        if (!badPackages.isEmpty()) {
            npmProject.getLogger().println(Arrays.toString(badPackages.toArray()));
            npmProject.getLogger().println("The npm dependencies above could not be found in Artifactory and therefore are not included in the build-info. " +
                    "Make sure the dependencies are available in Artifactory for this build.");
        }
        return new ArrayList<>(dependencies.values());
    }


    private void populateDependencies(Pair<NpmScope, JsonNode> dependencies) {
        DefaultMutableTreeNode rootNode = NpmDependencyTree.getDependenciesTree(dependencies.getKey(), dependencies.getValue());
        Enumeration e = rootNode.breadthFirstEnumeration();
        while (e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            PackageInfo packageInfo = (PackageInfo) node.getUserObject();
            if (packageInfo == null) {
                continue;
            }
            if (StringUtils.isBlank(packageInfo.getVersion())) {
                npmProject.getLogger().println("npm dependencies list contains the package " + packageInfo.getName() + " without version information. The dependency will not be added to build-info");
                continue;
            }

            if (!badPackages.contains(packageInfo)) {
                if (!appendDependency(packageInfo)) {
                    badPackages.add(packageInfo);
                }
            }
        }
    }

    private boolean appendDependency(PackageInfo packageInfo) {
        String id = packageInfo.getName() + ":" + packageInfo.getVersion();
        if (!dependencies.containsKey(id)) {
            Dependency dependency = createDependency(packageInfo);
            if (dependency == null) {
                return false;
            }
            dependencies.put(id, createDependency(packageInfo));
        } else {
            dependencies.get(id).getScopes().addAll(packageInfo.getScopes());
        }
        return true;
    }

    private Dependency createDependency(PackageInfo packageInfo) {
        DependencyBuilder builder = new DependencyBuilder().scopes(packageInfo.getScopes());
        String aql = String.format(NPM_AQL_FORMAT, packageInfo.getName(), packageInfo.getVersion());
        AqlSearchResult searchResult;
        try {
            searchResult = npmProject.getDependenciesClient().searchArtifactsByAql(aql);
            if (searchResult.getResults().isEmpty()) {
                return null;
            }
            AqlSearchResult.SearchEntry searchEntry = searchResult.getResults().get(0);
            return builder.id(searchEntry.getName()).
                    scopes(packageInfo.getScopes()).
                    md5(searchEntry.getActualMd5()).
                    sha1(searchEntry.getActualSha1()).
                    build();
        } catch (IOException e) {
            e.printStackTrace(npmProject.getLogger());
            return null;
        }
    }
}
