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

package org.jfrog.build.client;

import static org.jfrog.build.client.ClientProperties.ARTIFACTORY_PREFIX;

/**
 * @author Tomer Cohen
 */
public interface ClientIvyProperties {

    String PROP_ARTIFACTORY_IVY_PREFIX = ARTIFACTORY_PREFIX + "ivy.";
    /**
     * Property for whether to publish Ivy descriptors.
     */
    String PROP_PUBLISH_IVY = PROP_ARTIFACTORY_IVY_PREFIX + "publish";

    String PROP_IVY_ARTIFACT_PATTERN = PROP_ARTIFACTORY_IVY_PREFIX + "artifact.pattern";

    String PROP_IVY_IVY_PATTERN = PROP_ARTIFACTORY_IVY_PREFIX + "ivy.pattern";

    String PROP_M2_COMPATIBLE = PROP_ARTIFACTORY_IVY_PREFIX + "ivy.m2compatible";
}