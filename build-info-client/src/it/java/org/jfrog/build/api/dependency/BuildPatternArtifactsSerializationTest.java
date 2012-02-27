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

import org.jfrog.build.api.builder.dependency.BuildPatternArtifactsBuilder;
import org.jfrog.build.api.builder.dependency.PatternArtifactBuilder;
import org.jfrog.build.util.JsonSerializer;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * @author jbaruch
 * @since 16/02/12
 */
public class BuildPatternArtifactsSerializationTest {

    @Test
    public void testBuildOutputsSerialisation() throws Exception {

        PatternResult bobZips = new PatternResult();
        bobZips.addArtifact(new PatternArtifactBuilder().uri("lib-releases-local:bob/aaa.zip").size(4354354).lastModifiedDate(new Date()).sha1("sha1").build());
        bobZips.addArtifact(new PatternArtifactBuilder().uri("lib-releases-local:bob/yyy/bbb.zip").size(78654).lastModifiedDate(new Date()).sha1("sha1").build());

        PatternResult prod = new PatternResult();
        prod.addArtifact(new PatternArtifactBuilder().uri("lib-releases-local:mmm.jar").size(345654).lastModifiedDate(new Date()).sha1("sha1").build());
        prod.addArtifact(new PatternArtifactBuilder().uri("345654").size(456546743).lastModifiedDate(new Date()).sha1("sha1").build());


        BuildPatternArtifacts buildPatternArtifacts = new BuildPatternArtifactsBuilder().buildName("foo").buildNumber("123")
                .patternResult(bobZips).patternResult(prod).build();

        String buildOutputsJson = new JsonSerializer<BuildPatternArtifacts>().toJSON(buildPatternArtifacts);
        System.out.println("buildOutputsJson = " + buildOutputsJson);
    }
}
