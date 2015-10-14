/*
 * Copyright (C) 2011 JFrog Ltd.
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

package org.jfrog.gradle.plugin.artifactory.extractor;

import org.gradle.api.Project;
import org.gradle.api.artifacts.PublishArtifact;
import org.jfrog.build.client.DeployDetails;

/**
 * Container class that associates a Gradle {@link PublishArtifact} to its corresponding {@link DeployDetails}
 *
 * @author Tomer Cohen
 */
public class GradleDeployDetails implements Comparable<GradleDeployDetails> {

    private final DeployDetails deployDetails;
    private final PublishArtifactInfo publishArtifact;
    private final Project project;

    public GradleDeployDetails(PublishArtifactInfo publishArtifact, DeployDetails deployDetails, Project project) {
        this.deployDetails = deployDetails;
        this.publishArtifact = publishArtifact;
        this.project = project;
    }

    public DeployDetails getDeployDetails() {
        return deployDetails;
    }

    public Project getProject() {
        return project;
    }

    public PublishArtifactInfo getPublishArtifact() {
        return publishArtifact;
    }
    
    public int compareTo(GradleDeployDetails that) {
        int result = 0;
        if (this.deployDetails == null) {
            result = that.deployDetails == null ? 0 : -1;
        } else {
            result = this.deployDetails.compareTo(that.deployDetails);
        }
        if (result == 0) {
            if (this.publishArtifact == null) {
                result = that.publishArtifact == null ? 0 : -1;
            } else {
                result = this.publishArtifact.compareTo(that.publishArtifact);
            }
        }
        if (result == 0) {
            if (this.project == null) {
                result = that.project == null ? 0 : -1;
            } else {
                result = this.project.compareTo(that.project);
            }
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GradleDeployDetails that = (GradleDeployDetails) o;

        if (deployDetails != null ? !deployDetails.equals(that.deployDetails) : that.deployDetails != null)
            return false;
        if (publishArtifact != null ? !publishArtifact.equals(that.publishArtifact) : that.publishArtifact != null)
            return false;
        return !(project != null ? !project.equals(that.project) : that.project != null);

    }

    @Override
    public int hashCode() {
        int result = deployDetails != null ? deployDetails.hashCode() : 0;
        result = 31 * result + (publishArtifact != null ? publishArtifact.hashCode() : 0);
        result = 31 * result + (project != null ? project.hashCode() : 0);
        return result;
    }
}
