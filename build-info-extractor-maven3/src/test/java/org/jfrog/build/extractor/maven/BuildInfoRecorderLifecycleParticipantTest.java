package org.jfrog.build.extractor.maven;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.easymock.EasyMock;
import org.eclipse.aether.RepositorySystemSession;
import org.jfrog.build.extractor.ci.BuildInfoConfigProperties;

import java.util.Properties;


/**
 * @author Noam Y. Tenne
 */
public class BuildInfoRecorderLifecycleParticipantTest {

    public void testParticipantImplementation() throws Exception {
        BuildInfoRecorder buildInfoRecorder = new BuildInfoRecorder(null, null, null);

        BuildInfoRecorderLifecycleParticipant participant = new BuildInfoRecorderLifecycleParticipant(buildInfoRecorder);

        PlexusContainer plexusContainerMock = EasyMock.createMock(PlexusContainer.class);

        RepositorySystemSession repositorySystemSession = EasyMock.createMock(RepositorySystemSession.class);
        MavenExecutionRequest requestMock = EasyMock.createMock(MavenExecutionRequest.class);

        Properties mockSessionProperties = new Properties();
        mockSessionProperties.setProperty(BuildInfoConfigProperties.ACTIVATE_RECORDER, "true");
        EasyMock.expect(requestMock.getSystemProperties()).andReturn(mockSessionProperties).once();
        EasyMock.expect(requestMock.getUserProperties()).andReturn(mockSessionProperties).once();

        AbstractExecutionListener existingListener = new AbstractExecutionListener();
        EasyMock.expect(requestMock.getExecutionListener()).andReturn(existingListener).times(1);

        EasyMock.expect(requestMock.getUserSettingsFile()).andReturn(null).once();

        EasyMock.expect(requestMock.setExecutionListener(buildInfoRecorder)).andReturn(null).once();
        EasyMock.replay(requestMock);

        MavenExecutionResult resultMock = EasyMock.createMock(MavenExecutionResult.class);

        MavenSession session = new MavenSession(plexusContainerMock, repositorySystemSession, requestMock, resultMock);

        //value is true
        participant.afterProjectsRead(session);
    }
}
