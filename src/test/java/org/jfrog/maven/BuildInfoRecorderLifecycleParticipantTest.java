package org.jfrog.maven;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.easymock.EasyMock;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;
import org.jfrog.build.extractor.maven.BuildInfoRecorderLifecycleParticipant;
import org.testng.annotations.Test;

import java.lang.reflect.Field;


/**
 * @author Noam Y. Tenne
 */
@Test(enabled = false)
public class BuildInfoRecorderLifecycleParticipantTest {

    public void testParticipantImplementation() throws Exception {
        BuildInfoRecorder buildInfoRecorder = new BuildInfoRecorder();

        BuildInfoRecorderLifecycleParticipant participant = new BuildInfoRecorderLifecycleParticipant();

        Class<BuildInfoRecorderLifecycleParticipant> participantClass = BuildInfoRecorderLifecycleParticipant.class;
        Field recorderField = participantClass.getDeclaredField("recorder");
        recorderField.set(participant, buildInfoRecorder);

        PlexusContainer plexusContainerMock = EasyMock.createMock(PlexusContainer.class);
        MavenExecutionRequest requestMock = EasyMock.createMock(MavenExecutionRequest.class);
        MavenExecutionResult resultMock = EasyMock.createMock(MavenExecutionResult.class);

        AbstractExecutionListener existingListener = new AbstractExecutionListener();
        EasyMock.expect(requestMock.getExecutionListener()).andReturn(existingListener).once();

        MavenSession session = new MavenSession(plexusContainerMock, requestMock, resultMock);
        participant.afterProjectsRead(session);

        EasyMock.verify(plexusContainerMock, requestMock, requestMock);
    }
}
