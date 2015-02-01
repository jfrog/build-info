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

package org.jfrog.build.api;

import com.google.common.collect.Lists;
import org.jfrog.build.api.builder.dependency.BuildDependencyBuilder;
import org.jfrog.build.api.dependency.BuildDependency;
import org.jfrog.build.api.release.PromotionStatus;
import org.jfrog.build.extractor.clientConfiguration.util.JsonSerializer;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import static org.jfrog.build.api.BuildType.GRADLE;

/**
 * @author jbaruch
 * @since 15/02/12
 */
public class BuildInfoSerializationTest {

    @Test
    public void testBuildInfoSerialization() throws Exception {
        String version = "1.2.0";
        String name = "moo";
        String number = "15";
        BuildType buildType = GRADLE;
        Agent agent = new Agent("pop", "1.6");
        long durationMillis = 6L;
        String principal = "bob";
        String artifactoryPrincipal = "too";
        String url = "mitz";
        String parentName = "pooh";
        String parentNumber = "5";
        String vcsRevision = "2421";
        List<Module> modules = Lists.newArrayList();
        List<PromotionStatus> statuses = Lists.newArrayList();
        List<BuildDependency> buildDependencies = Arrays.asList(
                new BuildDependencyBuilder().name("foo").number("123").startedDate(new Date()).build(),
                new BuildDependencyBuilder().name("bar").number("456").startedDate(new Date()).build()
        );
        Properties properties = new Properties();

        Build build = new Build();
        build.setVersion(version);
        build.setName(name);
        build.setNumber(number);
        build.setType(buildType);
        build.setAgent(agent);
        build.setDurationMillis(durationMillis);
        build.setPrincipal(principal);
        build.setArtifactoryPrincipal(artifactoryPrincipal);
        build.setUrl(url);
        build.setParentName(parentName);
        build.setParentNumber(parentNumber);
        build.setModules(modules);
        build.setStatuses(statuses);
        build.setProperties(properties);
        build.setVcsRevision(vcsRevision);
        build.setBuildDependencies(buildDependencies);

        String buildInfoJSON = new JsonSerializer<Build>().toJSON(build);
        System.out.println("buildInfoJSON = " + buildInfoJSON);

    }
}
