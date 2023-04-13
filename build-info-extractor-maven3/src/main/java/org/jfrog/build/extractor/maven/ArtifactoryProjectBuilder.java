package org.jfrog.build.extractor.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jfrog.build.extractor.maven.resolver.ResolutionHelper;
import org.sonatype.aether.repository.RepositoryPolicy;

import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED;

@Named
@Component(role = DefaultProjectBuilder.class, hint = "default")
public class ArtifactoryProjectBuilder extends DefaultProjectBuilder {

    @Requirement
    private ResolutionHelper resolutionHelper;

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request) throws ProjectBuildingException {
        if (Boolean.parseBoolean(System.getProperties().getProperty(PROP_ARTIFACTORY_RESOLUTION_ENABLED))
                || Boolean.parseBoolean(System.getenv(PROP_ARTIFACTORY_RESOLUTION_ENABLED))) {
            if (!resolutionHelper.isInitialized()) {
                Properties allMavenProps = new Properties();
                allMavenProps.putAll(request.getSystemProperties());
                allMavenProps.putAll(request.getUserProperties());
                resolutionHelper.init(allMavenProps);
            }

            // We're setting the resolver repositories to the list of repositories.
            // This repository replaces the central repository.
            // This ensures that parent poms with snapshot versions can be downloaded from Artifactory.
            List<ArtifactRepository> repositories = getRepositories();
            request.setRemoteRepositories(repositories);
            request.setPluginArtifactRepositories(repositories);
        }
        return super.build(pomFiles, recursive, request);
    }

    private List<ArtifactRepository> getRepositories() {
        List<ArtifactRepository> repositories = new ArrayList<>();

        String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
        String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

        org.apache.maven.artifact.repository.Authentication authentication = null;
        if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
            authentication = new org.apache.maven.artifact.repository.Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
        }
        org.apache.maven.repository.Proxy proxy = null;
        if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
            proxy = new org.apache.maven.repository.Proxy();
            proxy.setHost(resolutionHelper.getProxyHost());
            proxy.setPort(resolutionHelper.getProxyPort());
            proxy.setUserName(resolutionHelper.getProxyUsername());
            proxy.setPassword(resolutionHelper.getProxyPassword());
        }

        if (StringUtils.isNotBlank(snapshotRepoUrl)) {
            ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(false, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            MavenArtifactRepository repository = new MavenArtifactRepository("artifactory-snapshot", snapshotRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);

            if (authentication != null) {
                repository.setAuthentication(authentication);
            }
            if (proxy != null) {
                repository.setProxy(proxy);
            }

            repositories.add(repository);
        }
        if (StringUtils.isNotBlank(releaseRepoUrl)) {
            boolean snapshotPolicyEnabled = StringUtils.isBlank(snapshotRepoUrl);
            String repositoryId = snapshotPolicyEnabled ? "artifactory-release-snapshot" : "artifactory-release";

            ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(snapshotPolicyEnabled, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_DAILY, RepositoryPolicy.CHECKSUM_POLICY_WARN);
            MavenArtifactRepository repository = new MavenArtifactRepository(repositoryId, releaseRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);

            if (authentication != null) {
                repository.setAuthentication(authentication);
            }
            if (proxy != null) {
                repository.setProxy(proxy);
            }

            repositories.add(repository);
        }
        return repositories;
    }
}
