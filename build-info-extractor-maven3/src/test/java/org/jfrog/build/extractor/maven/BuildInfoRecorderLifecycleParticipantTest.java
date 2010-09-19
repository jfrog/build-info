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

package org.jfrog.build.extractor.maven;

import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;
import org.easymock.EasyMock;
import org.sonatype.aether.RepositorySystemSession;
import org.testng.Assert;

import java.lang.reflect.Field;
import java.util.Properties;


/**
 * @author Noam Y. Tenne
 */
public class BuildInfoRecorderLifecycleParticipantTest {

    public void testParticipantImplementation() throws Exception {
        BuildInfoRecorder buildInfoRecorder = new BuildInfoRecorder();

        BuildInfoRecorderLifecycleParticipant participant = new BuildInfoRecorderLifecycleParticipant();

        Class<BuildInfoRecorderLifecycleParticipant> participantClass = BuildInfoRecorderLifecycleParticipant.class;
        Field recorderField = participantClass.getDeclaredField("recorder");
        recorderField.set(participant, buildInfoRecorder);

        Field loggerField = participantClass.getDeclaredField("logger");
        loggerField.setAccessible(true);
        loggerField.set(participant, new AbstractLogger(1, "dummy") {

            public void debug(String message, Throwable throwable) {
                Assert.assertTrue(message.contains("value is true"));
            }

            public void info(String message, Throwable throwable) {
                assert false;
            }

            public void warn(String message, Throwable throwable) {
                assert false;
            }

            public void error(String message, Throwable throwable) {
                assert false;
            }

            public void fatalError(String message, Throwable throwable) {
                assert false;
            }

            public Logger getChildLogger(String name) {
                assert false;
                return null;
            }
        });

        PlexusContainer plexusContainerMock = EasyMock.createMock(PlexusContainer.class);

        RepositorySystemSession repositorySystemSession = EasyMock.createMock(RepositorySystemSession.class);
        MavenExecutionRequest requestMock = EasyMock.createMock(MavenExecutionRequest.class);

        Properties mockSessionProperties = new Properties();
        mockSessionProperties.setProperty(BuildInfoRecorder.ACTIVATE_RECORDER, "true");
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
