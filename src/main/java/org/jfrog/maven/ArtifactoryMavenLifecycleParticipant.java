package org.jfrog.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

/**
 * @author Noam Y. Tenne
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ArtifactoryMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement
    private Logger logger;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        ExecutionListener existingExecutionListener = session.getRequest().getExecutionListener();
        ArtifactoryExecutionListener artifactoryExecutionListener =
                new ArtifactoryExecutionListener(logger, existingExecutionListener);
        session.getRequest().setExecutionListener(artifactoryExecutionListener);
    }
}
