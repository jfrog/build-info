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
 * Main interface of the build file bean
 *
 * @author Noam Y. Tenne
 */
public interface BuildFileBean extends BuildBean {

    /**
     * Returns the SHA1 checksum of the file
     *
     * @return File SHA1 checksum
     */
    String getSha1();

    /**
     * Sets the SHA1 checksum of the file
     *
     * @param sha1 File SHA1 checksum
     */
    void setSha1(String sha1);

    /**
     * Returns the SHA256 checksum of the file
     *
     * @return File SHA256 checksum
     */
    String getSha256();

    /**
     * Sets the SHA256 checksum of the file
     *
     * @param sha256 File SHA256 checksum
     */
    void setSha256(String sha256);

    /**
     * Returns the MD5 checksum of the file
     *
     * @return File MD5 checksum
     */
    String getMd5();

    /**
     * Sets the MD5 checksum of the file
     *
     * @param md5 File MD5 checksum
     */
    void setMd5(String md5);

    /**
     * Returns the type of the file
     *
     * @return File type
     */
    String getType();

    /**
     * Sets the type of the file
     *
     * @param type File type
     */
    void setType(String type);
}