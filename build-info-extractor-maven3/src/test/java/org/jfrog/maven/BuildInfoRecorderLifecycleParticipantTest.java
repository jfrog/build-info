/*
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
