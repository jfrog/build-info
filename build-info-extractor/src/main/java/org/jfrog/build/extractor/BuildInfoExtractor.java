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

package org.jfrog.build.extractor;

import org.jfrog.build.api.ci.BuildInfo;

/**
 * -DbuildInfo.property.name=value -DbuildInfo.deploy.property.name=value (becomes a matrix param in the current impl)
 * and - -DbuildInfo.propertiesFile=/path/to/file (contains the above properties without the buildInfo prefix)
 *
 * @author Noam Y. Tenne
 */
public interface BuildInfoExtractor<C> {

    /**
     * <ol> <li>Collect the props (from -D props and the props supplied in the {@link
     * org.jfrog.build.api.BuildInfoConfigProperties#PROP_PROPS_FILE} file.</li>
     *
     * <li>Collect published artifacts and dependency artifacts produced/used by the underlying build technology, based
     * on the context.</li> </ol>
     *
     * @param buildInfoTask
     * @return A handle for the exported buildInfo
     */
    BuildInfo extract(C buildInfoTask) throws Exception;
}
