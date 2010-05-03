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

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.ExecutionListener;
import org.apache.maven.execution.MavenSession;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

/**
 * @author Noam Y. Tenne
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class BuildInfoRecorderLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement(role = BuildInfoRecorder.class, hint = "default", optional = false)
    BuildInfoRecorder recorder;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        ExecutionListener existingExecutionListener = session.getRequest().getExecutionListener();
        recorder.setListenerToWrap(existingExecutionListener);
        session.getRequest().setExecutionListener(recorder);
    }
}
