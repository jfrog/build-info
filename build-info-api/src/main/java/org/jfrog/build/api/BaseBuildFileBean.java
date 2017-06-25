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

/**
 * Base implementation of the build file bean interface
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseBuildFileBean extends BaseBuildBean implements BuildFileBean {

    protected String type;
    protected String sha1;
    protected String sha2;
    protected String md5;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getSha2() {
        return sha2;
    }

    public void setSha2(String sha2) {
        this.sha2 = sha2;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseBuildFileBean)) {
            return false;
        }
        BaseBuildFileBean that = (BaseBuildFileBean) o;
        if (md5 != null ? !md5.equals(that.md5) : that.md5 != null) {
            return false;
        }
        if (sha1 != null ? !sha1.equals(that.sha1) : that.sha1 != null) {
            return false;
        }
        if (sha2 != null ? !sha2.equals(that.sha2) : that.sha2 != null) {
            return false;
        }
        if (type != null ? !type.equals(that.type) : that.type != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (sha1 != null ? sha1.hashCode() : 0);
        result = 31 * result + (sha2 != null ? sha2.hashCode() : 0);
        result = 31 * result + (md5 != null ? md5.hashCode() : 0);
        return result;
    }
}