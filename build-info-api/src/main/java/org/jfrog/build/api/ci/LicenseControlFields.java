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
package org.jfrog.build.api.ci;

/**
 * @author freds
 */
public interface LicenseControlFields {
    String RUN_CHECKS = "runChecks";
    String VIOLATION_RECIPIENTS = "violationRecipients";
    String INCLUDE_PUBLISHED_ARTIFACTS = "includePublishedArtifacts";
    String SCOPES = "scopes";
    String AUTO_DISCOVER = "autoDiscover";
}
