package org.jfrog.build.extractor.maven;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.*;
import org.codehaus.plexus.component.annotations.Component;

import javax.inject.Named;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jfrog.build.api.BuildInfoConfigProperties.PROP_ARTIFACTORY_RESOLUTION_ENABLED;

@Named
@Component( role = DefaultProjectBuilder.class, hint = "default")
public class ArtifactoryProjectBuilder extends DefaultProjectBuilder {

    @Override
    public List<ProjectBuildingResult> build(List<File> pomFiles, boolean recursive, ProjectBuildingRequest request) throws ProjectBuildingException {
        if (Boolean.parseBoolean(System.getProperties().getProperty(PROP_ARTIFACTORY_RESOLUTION_ENABLED))
                || Boolean.parseBoolean(System.getenv(PROP_ARTIFACTORY_RESOLUTION_ENABLED))) {
            // We're setting a dummy repository to the list of repositories, which has both snapshot and release policies enabled.
            // This repository replaces the central repository.
            // This ensures that parent poms with snapshot versions can be downloaded from Artifactory.
            request.setRemoteRepositories(getDummyRepo());
        }
        return super.build(pomFiles, recursive, request);
    }

    private List<ArtifactRepository> getDummyRepo() {
        ArtifactRepository repo = new MavenArtifactRepository("dummy",
                "http://ArtifactoryDummyRepo./dummy",
                new DefaultRepositoryLayout(),
                new ArtifactRepositoryPolicy(),
                new ArtifactRepositoryPolicy());
        return new ArrayList<ArtifactRepository>(Collections.singletonList(repo));
    }
}
