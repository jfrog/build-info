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

package org.jfrog.build.util;

/**
 * Special checked exception to hold relevant information of version resolution failures
 *
 * @author Noam Y. Tenne
 */
public class VersionException extends Exception {

    private VersionCompatibilityType versionCompatibilityType;

    public VersionException(String message, VersionCompatibilityType versionCompatibilityType) {
        super(message);
        this.versionCompatibilityType = versionCompatibilityType;
    }

    public VersionException(String message, Throwable cause, VersionCompatibilityType versionCompatibilityType) {
        super(message, cause);
        this.versionCompatibilityType = versionCompatibilityType;
    }

    public VersionCompatibilityType getVersionCompatibilityType() {
        return versionCompatibilityType;
    }
}
