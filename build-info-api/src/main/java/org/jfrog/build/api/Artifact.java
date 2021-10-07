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

package org.jfrog.build.api;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.apache.commons.lang3.StringUtils;

/**
 * Contains the build deployed artifact information
 *
 * @author Noam Y. Tenne
 */
@XStreamAlias(BuildBean.ARTIFACT)
public class Artifact extends BaseBuildFileBean {

    private String name;

    /**
     * Returns the name of the artifact
     *
     * @return Artifact name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the artifact
     *
     * @param name Artifact name
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        Artifact artifact = (Artifact) o;
        return StringUtils.equals(name, artifact.name) && StringUtils.equals(remotePath, artifact.remotePath);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public org.jfrog.build.api.ci.Artifact ToBuildInfoArtifact() {
        org.jfrog.build.api.ci.Artifact result = new org.jfrog.build.api.ci.Artifact();
        result.setName(name);
        result.setMd5(md5);
        result.setSha256(sha256);
        result.setSha1(sha1);
        result.setRemotePath(remotePath);
        result.setProperties(getProperties());
        return result;
    }
}