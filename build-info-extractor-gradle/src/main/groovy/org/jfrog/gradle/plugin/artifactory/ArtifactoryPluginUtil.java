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

package org.jfrog.gradle.plugin.artifactory;

import org.gradle.api.Project;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;

public class ArtifactoryPluginUtil {

    /**
     * Get Artifactory plugin convention
     * @param project
     * @return the first concrete plugin convention up the project modules tree.
     */
    public static ArtifactoryPluginConvention getArtifactoryConvention(Project project) {
        ArtifactoryPluginConvention acc = null;
        while (project != null) {
            acc = project.getConvention().getPlugin(ArtifactoryPluginConvention.class);
            if (acc != null && acc.getConventionSet()) {
                return acc;
            }

            project = project.getParent();
        }
        return acc;
    }
}