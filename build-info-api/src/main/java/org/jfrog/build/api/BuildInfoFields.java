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
package org.jfrog.build.api;

/**
 * @author freds
 */
public interface BuildInfoFields {
    String BUILD_NAME = "build.name";
    String BUILD_NUMBER = "build.number";
    String BUILD_TIMESTAMP = "build.timestamp";
    String BUILD_STARTED = "build.started";
    String BUILD_PARENT_NAME = "build.parentName";
    String BUILD_PARENT_NUMBER = "build.parentNumber";
    String VCS_REVISION = "vcs.revision";
    String PRINCIPAL = "principal";
    String BUILD_URL = "buildUrl";
    String BUILD_AGENT_NAME = "buildAgent.name";
    String BUILD_AGENT_VERSION = "buildAgent.version";
    String AGENT_NAME = "agent.name";
    String AGENT_VERSION = "agent.version";
    String OUTPUT_FILE = "output.file";
    String ENVIRONMENT_PREFIX = "env.";
    String BUILD_RETENTION_DAYS = "buildRetention.daysToKeep";
    String BUILD_RETENTION_MINIMUM_DATE = "buildRetention.minimumDate";
    String RELEASE_ENABLED = "promotion.enabled";
    String RELEASE_COMMENT = "promotion.comment";
}
