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

package org.jfrog.build.extractor.maven;

import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.client.*;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Noam Y. Tenne
 */
@Component(role = BuildDeploymentHelper.class)
public class BuildDeploymentHelper {

    @Requirement
    private Logger logger;

    @Requirement
    private BuildInfoClientBuilder buildInfoClientBuilder;


    private final JsonMergeHelper buildInfoMergeHelper   = new JsonMergeHelper( "id", "name" );
    private final JsonMergeHelper deployablesMergeHelper = new JsonMergeHelper( "file" );


    public void deploy( Build                          build,
                        ArtifactoryClientConfiguration clientConf,
                        Map<String, DeployDetails>     deployableArtifactBuilders,
                        boolean                        wereThereTestFailures,
                        File                           basedir )
    {

        Set<DeployDetails> deployableArtifacts = prepareDeployableArtifacts(build, deployableArtifactBuilders);
        String             outputFile          = clientConf.getExportFile();
        File               buildInfoFile       = StringUtils.isBlank( outputFile ) ?
                                                    new File( basedir, "target/build-info.json" ) :
                                                    new File( outputFile );
        File aggregateDirectory  = null;
        File buildInfoAggregated = null;

        logger.debug("Build Info Recorder: " + BuildInfoConfigProperties.EXPORT_FILE + " = " + outputFile);
        logger.info( "Artifactory Build Info Recorder: Saving Build Info to '" + buildInfoFile + "'" );

        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, buildInfoFile);
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while persisting Build Info to '" + buildInfoFile + "'", e);
        }

        logger.debug("Build Info Recorder: " + clientConf.publisher.isPublishBuildInfo() + " = " + clientConf.publisher.isPublishBuildInfo());
        logger.debug("Build Info Recorder: " + clientConf.publisher.isPublishArtifacts() + " = " + clientConf);

        if ( clientConf.publisher.getAggregateArtifacts() != null ){
            aggregateDirectory  = new File( clientConf.publisher.getAggregateArtifacts());
            buildInfoAggregated = new File( aggregateDirectory, "build-info.json" );
            deployableArtifacts = aggregateArtifacts( basedir, aggregateDirectory, buildInfoFile, buildInfoAggregated, deployableArtifacts );

            if ( ! clientConf.publisher.isPublishAggregatedArtifacts()) {
                return;
            }
        }

        if (clientConf.publisher.isPublishBuildInfo() || clientConf.publisher.isPublishArtifacts()) {
            ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf);
            boolean isDeployArtifacts = clientConf.publisher.isPublishArtifacts() &&
                                        ( deployableArtifacts != null )           &&
                                        ( ! deployableArtifacts.isEmpty())        &&
                                        ( clientConf.publisher.isEvenUnstable() || ( ! wereThereTestFailures ));
            boolean isSendBuildInfo   = clientConf.publisher.isPublishBuildInfo() &&
                                        ( clientConf.publisher.isEvenUnstable() || ( ! wereThereTestFailures ));

            try {
                if ( isDeployArtifacts ) {
                    deployArtifacts(clientConf.publisher, deployableArtifacts, client);
                }

                if ( isSendBuildInfo ) {
                    try {

                        if ( buildInfoAggregated != null ) {
                            String buildInfoJson = client.buildInfoToJsonString( build );
//                            buildInfoMergeHelper.mergeMaps(  )
                        }

                        logger.info("Artifactory Build Info Recorder: Deploying build info ...");
                        client.sendBuildInfo(build);
                    } catch (Exception e) {
                        throw new RuntimeException("Error occurred while publishing Build Info to Artifactory.", e);
                    }
                }
            } finally {
                client.shutdown();
            }
        }
    }


    private Set<DeployDetails> aggregateArtifacts ( File basedir, File aggregateDirectory, File buildInfoSource, File buildInfoDestination, Iterable<DeployDetails> deployables ){
        try {
            File deployablesDestination = new File( aggregateDirectory, "deployables.json" );
            List<Map<String,?>> mergedDeployables;

            if ( buildInfoDestination.isFile()) {
                Map<String,Object> buildInfoSourceMap      = buildInfoMergeHelper.jsonToObject( buildInfoSource,      Map.class );
                Map<String,Object> buildInfoDestinationMap = buildInfoMergeHelper.jsonToObject( buildInfoDestination, Map.class );
                buildInfoSourceMap.put( "started", buildInfoDestinationMap.get( "started" ));
                buildInfoSourceMap.put( "durationMillis", ( Integer ) buildInfoDestinationMap.get( "durationMillis" ) +
                                                          ( Integer ) buildInfoSourceMap.get( "durationMillis" ));
                buildInfoMergeHelper.mergeAndWrite( buildInfoSourceMap, buildInfoDestinationMap, buildInfoDestination );
            }
            else {
                FileUtils.copyFile( buildInfoSource, buildInfoDestination );
            }

            if ( deployablesDestination.isFile()) {
                List<Map<String,?>> currentDeployables  = deployablesMergeHelper.jsonToObject ( deployablesMergeHelper.objectToJson( deployables ), List.class );
                List<Map<String,?>> previousDeployables = deployablesMergeHelper.jsonToObject ( deployablesDestination, List.class );
                mergedDeployables = deployablesMergeHelper.mergeAndWrite( currentDeployables, previousDeployables, deployablesDestination );
            }
            else {
                deployablesMergeHelper.jsonWrite( deployables, deployablesDestination );
                mergedDeployables = deployablesMergeHelper.jsonToObject( deployablesDestination, List.class );
            }

            String basedirPath = basedir.getCanonicalPath().replace( '\\', '/' );

            for ( DeployDetails details : deployables ) {
                /**
                 * We could check MD5 checksum of destination file (if it exists) and save on copy operation but since most *.jar
                 * files contain a timestamp in pom.properties (thanks, Maven) - checksum would only match for POM files.
                 */
                File aggregatedFile = aggregatedFile( basedirPath, aggregateDirectory, details.getFile());
                FileUtils.copyFile( details.getFile(), aggregatedFile );
            }

            return convertDeployables( basedirPath, aggregateDirectory, mergedDeployables );
        }
        catch ( IOException e ){
            throw new RuntimeException( "Failed to aggregate artifacts and Build Info in [" + aggregateDirectory + "]",
                                        e );
        }
    }


    @SuppressWarnings({ "FeatureEnvy" , "SuppressionAnnotation" })
    private Set<DeployDetails> convertDeployables ( String basedirPath, File aggregateDirectory, Iterable<Map<String, ?>> deployables ) throws IOException
    {
        Set<DeployDetails> result = new HashSet<DeployDetails>();

        for ( Map<String,?> map : deployables ) {

            DeployDetails.Builder builder = new DeployDetails.Builder().
                                            targetRepository(( String ) map.get( "targetRepository" )).
                                            artifactPath(( String ) map.get( "artifactPath" )).
                                            file( aggregatedFile( basedirPath, aggregateDirectory, new File(( String ) map.get( "file" )))).
                                            sha1(( String ) map.get( "sha1" )).
                                            md5(( String ) map.get( "md5" )).
                                            addProperties(( Map<String, String> ) map.get( "properties" ));
            result.add( builder.build());
        }

        return result;
    }


    private File aggregatedFile( String basedirPath, File aggregateDirectory, File file ) throws IOException
    {
        String artifactPath         = file.getCanonicalPath().replace( '\\', '/' );
        String artifactRelativePath = artifactPath.startsWith( basedirPath ) ?
           /**
            * "/Users/evgenyg/.hudson/jobs/teamcity-artifactory-plugin/workspace/agent/target/teamcity-artifactory-plugin-agent-2.1.x-SNAPSHOT.jar" =>
            * "agent/target/teamcity-artifactory-plugin-agent-2.1.x-SNAPSHOT.jar"
            */
            artifactPath.substring( basedirPath.length() + 1 ) :
           /**
            * Artifact is outside workspace, wonder if it works on Windows
            */
            artifactPath;

        return new File( aggregateDirectory, artifactRelativePath );
    }


    private Set<DeployDetails> prepareDeployableArtifacts(Build build,
            Map<String, DeployDetails> deployableArtifactBuilders) {
        Set<DeployDetails> deployableArtifacts = Sets.newLinkedHashSet();
        List<Module> modules = build.getModules();
        for (Module module : modules) {
            List<Artifact> artifacts = module.getArtifacts();
            for (Artifact artifact : artifacts) {
                String artifactId = BuildInfoExtractorUtils.getArtifactId(module.getId(), artifact.getName());
                DeployDetails deployable = deployableArtifactBuilders.get(artifactId);
                if (deployable != null) {
                    File file = deployable.getFile();
                    setArtifactChecksums(file, artifact);
                    deployableArtifacts.add(new DeployDetails.Builder().artifactPath(deployable.getArtifactPath()).
                            file(file).md5(artifact.getMd5()).sha1(artifact.getSha1()).
                            addProperties(deployable.getProperties()).
                            targetRepository(deployable.getTargetRepository()).build());
                }
            }
        }
        return deployableArtifacts;
    }

    private void deployArtifacts(ArtifactoryClientConfiguration.PublisherHandler publishConf,
            Set<DeployDetails> deployableArtifacts,
            ArtifactoryBuildInfoClient client) {
        logger.info("Artifactory Build Info Recorder: Deploying artifacts to " + publishConf.getUrl());

        IncludeExcludePatterns includeExcludePatterns = getArtifactDeploymentPatterns(publishConf);
        for (DeployDetails artifact : deployableArtifacts) {
            String artifactPath = artifact.getArtifactPath();
            if (PatternMatcher.pathConflicts(artifactPath, includeExcludePatterns)) {
                logger.info("Artifactory Build Info Recorder: Skipping the deployment of '" +
                        artifactPath + "' due to the defined include-exclude patterns.");
                continue;
            }

            try {
                client.deployArtifact(artifact);
            } catch (IOException e) {
                throw new RuntimeException("Error occurred while publishing artifact to Artifactory: " +
                        artifact.getFile() +
                        ".\n Skipping deployment of remaining artifacts (if any) and build info.", e);
            }
        }
    }

    private void setArtifactChecksums(File artifactFile, org.jfrog.build.api.Artifact artifact) {
        if ((artifactFile != null) && (artifactFile.isFile())) {
            try {
                Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "md5", "sha1");
                artifact.setMd5(checksums.get("md5"));
                artifact.setSha1(checksums.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + artifact.getName() + "': " + e.getMessage(), e);
            }
        }
    }

    private IncludeExcludePatterns getArtifactDeploymentPatterns(
            ArtifactoryClientConfiguration.PublisherHandler publishConf) {
        return new IncludeExcludePatterns(publishConf.getIncludePatterns(), publishConf.getExcludePatterns());
    }
}
