/*
 * Copyright (C) 2012 JFrog Ltd.
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

package org.jfrog.build.api.dependency;

import org.jfrog.build.api.builder.dependency.BuildPatternArtifactsRequestBuilder;
import org.jfrog.build.util.JsonSerializer;
import org.testng.annotations.Test;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsRequestSerializationTest {

    @Test
    public void testBuildOutputsRequestSerialisation() throws Exception {
        BuildPatternArtifactsRequest request = new BuildPatternArtifactsRequestBuilder().buildName("foo").buildNumber("LATEST")
                .pattern("*:dir/*/bob/**/*.zip").pattern("*:**/*.*;status+=prod").build();

        String requstJson = new JsonSerializer<BuildPatternArtifactsRequest>().toJSON(request);
        System.out.println("requstJson = " + requstJson);
    }
}
