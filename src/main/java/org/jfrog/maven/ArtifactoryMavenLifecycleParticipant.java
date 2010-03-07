package org.jfrog.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;

/**
 * @author Noam Y. Tenne
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ArtifactoryMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        ExecutionListener existingExecutionListener = session.getRequest().getExecutionListener();
        ArtifactoryExecutionListener artifactoryExecutionListener =
                new ArtifactoryExecutionListener(existingExecutionListener);
        session.getRequest().setExecutionListener(artifactoryExecutionListener);
    }
}
