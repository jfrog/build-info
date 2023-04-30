package org.jfrog.build.extractor.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.DefaultProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.jfrog.build.extractor.maven.resolver.ArtifactoryPluginResolution;
import org.jfrog.build.extractor.maven.resolver.NullPlexusLog;
import org.jfrog.build.extractor.maven.resolver.ResolutionHelper;

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

        ArtifactoryPluginResolution artifactoryResolution = new ArtifactoryPluginResolution(resolutionHelper.getRepoReleaseUrl(), resolutionHelper.getRepoSnapshotUrl(), resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword(), new NullPlexusLog());
        ArtifactRepository snapshotRepository = artifactoryResolution.createSnapshotRepository();
        if (snapshotRepository != null) {
            repositories.add(snapshotRepository);
        }
        ArtifactRepository releaseRepository = artifactoryResolution.createReleaseRepository();
        if (releaseRepository != null) {
            repositories.add(releaseRepository);
        }
        return repositories;
    }
}
