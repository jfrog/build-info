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
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.build.extractor.retention.Utils;

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

    private final JsonMergeHelper buildInfoMergeHelper   = new JsonMergeHelper( "id", "name" );
    private final JsonMergeHelper deployablesMergeHelper = new JsonMergeHelper( "artifactPath" );
    @Requirement
    private Logger logger;
    @Requirement
    private BuildInfoClientBuilder buildInfoClientBuilder;

    public void deploy( Build                          build,
                        ArtifactoryClientConfiguration clientConf,
                        Map<String, DeployDetails>     deployableArtifactBuilders,
                        boolean                        wereThereTestFailures,
                        File                           basedir ) {

        Set<DeployDetails> deployableArtifacts = prepareDeployableArtifacts(build, deployableArtifactBuilders);

        logger.debug("Build Info Recorder: deploy artifacts: " + clientConf.publisher.isPublishArtifacts());
        logger.debug("Build Info Recorder: publish build info: " + clientConf.publisher.isPublishBuildInfo());

        File aggregateDirectory;
        File buildInfoAggregated = null;
        File buildInfoFile = null;
        if (clientConf.publisher.isPublishBuildInfo() || clientConf.publisher.getAggregateArtifacts() != null) {
            buildInfoFile = saveBuildInfoToFile(build, clientConf, basedir);
        }
        if (clientConf.publisher.getAggregateArtifacts() != null) {
            aggregateDirectory                   = new File( clientConf.publisher.getAggregateArtifacts());
            buildInfoAggregated                  = new File( aggregateDirectory, "build-info.json" );
            boolean isCopyAggregatedArtifacts    = clientConf.publisher.isCopyAggregatedArtifacts();
            boolean isPublishAggregatedArtifacts = clientConf.publisher.isPublishAggregatedArtifacts();
            deployableArtifacts                  = aggregateArtifacts( aggregateDirectory, buildInfoFile, buildInfoAggregated, deployableArtifacts,
                                                                       isCopyAggregatedArtifacts, isPublishAggregatedArtifacts );

            if (!isPublishAggregatedArtifacts) {
                return;
            }
        }

        if (!StringUtils.isEmpty(clientConf.info.getGeneratedBuildInfoFilePath())) {
            try {
                BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(clientConf.info.getGeneratedBuildInfoFilePath()));
            } catch (Exception e) {
                logger.error("Failed writing build info to file: " , e);
                throw new RuntimeException("Failed writing build info to file", e);
            }
        }

        if (!StringUtils.isEmpty(clientConf.info.getDeployableArtifactsFilePath())) {
            try {
                DeployableArtifactsUtils.saveDeployableArtifactsToFile(deployableArtifacts, new File(clientConf.info.getDeployableArtifactsFilePath()));
            } catch (Exception e) {
                logger.error("Failed writing deployable artifacts to file: ", e);
                throw new RuntimeException("Failed writing deployable artifacts to file", e);
            }
        }

        if (isDeployArtifacts(clientConf, wereThereTestFailures, deployableArtifacts)) {
            deployArtifacts(clientConf, deployableArtifacts);
        }
        if (isPublishBuildInfo(clientConf, wereThereTestFailures)) {
            publishBuildInfo(clientConf, build, buildInfoAggregated);
        }
    }

    private void publishBuildInfo(ArtifactoryClientConfiguration clientConf, Build build, File buildInfoAggregated) {
        ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf);
        logger.info("Artifactory Build Info Recorder: Deploying build info ...");
        try {
            if (buildInfoAggregated != null) {
                String buildInfoJson = client.buildInfoToJsonString(build);
                String buildInfoAggregatedJson = FileUtils.readFileToString(buildInfoAggregated, "UTF-8");
                String buildInfoMerged = buildInfoMergeHelper.mergeJsons(buildInfoAggregatedJson, buildInfoJson);
                client.sendBuildInfo(buildInfoMerged);
            } else {
                Utils.sendBuildAndBuildRetention(client, build, clientConf);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            client.close();
        }
    }

    private boolean isDeployArtifacts(ArtifactoryClientConfiguration clientConf, boolean wereThereTestFailures, Set<DeployDetails> deployableArtifacts) {
        if (!clientConf.publisher.isPublishArtifacts()) {
            logger.info("Artifactory Build Info Recorder: deploy artifacts set to false, artifacts will not be deployed...");
            return false;
        }
        if (deployableArtifacts == null || deployableArtifacts.isEmpty()) {
            logger.info("Artifactory Build Info Recorder: no artifacts to deploy...");
            return false;
        }
        if (wereThereTestFailures && !clientConf.publisher.isEvenUnstable()) {
            logger.warn("Artifactory Build Info Recorder: unstable build, artifacts will not be deployed...");
            return false;
        }
        return true;
    }

    private boolean isPublishBuildInfo(ArtifactoryClientConfiguration clientConf, boolean wereThereTestFailures) {
        if (!clientConf.publisher.isPublishBuildInfo()) {
            logger.info("Artifactory Build Info Recorder: publish build info set to false, build info will not be published...");
            return false;
        }
        if (wereThereTestFailures && !clientConf.publisher.isEvenUnstable()) {
            logger.warn("Artifactory Build Info Recorder: unstable build, build info will not be published...");
            return false;
        }
        return true;
    }

    private File saveBuildInfoToFile(Build build, ArtifactoryClientConfiguration clientConf, File basedir) {
        String outputFile = clientConf.getExportFile();
        File buildInfoFile = StringUtils.isBlank(outputFile) ? new File(basedir, "target/build-info.json" ) :
                new File(outputFile);

        logger.debug("Build Info Recorder: " + BuildInfoConfigProperties.EXPORT_FILE + " = " + outputFile);
        logger.info("Artifactory Build Info Recorder: Saving Build Info to '" + buildInfoFile + "'" );

        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, buildInfoFile.getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while persisting Build Info to '" + buildInfoFile + "'", e);
        }
        return buildInfoFile;
    }

    @SuppressWarnings({ "TypeMayBeWeakened" , "SuppressionAnnotation" })
    private Set<DeployDetails> aggregateArtifacts ( File               aggregateDirectory,
                                                    File               buildInfoSource,
                                                    File               buildInfoDestination,
                                                    Set<DeployDetails> deployables,
                                                    boolean            isCopyAggregatedArtifacts,
                                                    boolean            isPublishAggregatedArtifacts ){
        try {
            File                deployablesDestination = new File( aggregateDirectory, "deployables.json" );
            List<Map<String,?>> mergedDeployables      = null;

            if ( buildInfoDestination.isFile()) {
                Map<String,Object> buildInfoSourceMap      = buildInfoMergeHelper.jsonToObject( buildInfoSource,      Map.class );
                Map<String,Object> buildInfoDestinationMap = buildInfoMergeHelper.jsonToObject( buildInfoDestination, Map.class );
                int durationMillis                         = ( Integer ) buildInfoSourceMap.get( "durationMillis" ) +
                                                             ( Integer ) buildInfoDestinationMap.get( "durationMillis" );
                buildInfoSourceMap.put( "started", buildInfoDestinationMap.get( "started" ));
                buildInfoSourceMap.put( "durationMillis", durationMillis );
                buildInfoMergeHelper.mergeAndWrite( buildInfoSourceMap, buildInfoDestinationMap, buildInfoDestination );
            }
            else {
                FileUtils.copyFile( buildInfoSource, buildInfoDestination );
            }

            if ( deployablesDestination.isFile()) {
                List<Map<String,?>> currentDeployables  = deployablesMergeHelper.jsonToObject ( deployablesMergeHelper.objectToJson( deployables ), List.class );
                List<Map<String,?>> previousDeployables = deployablesMergeHelper.jsonToObject ( deployablesDestination, List.class );
                mergedDeployables                       = deployablesMergeHelper.mergeAndWrite( currentDeployables, previousDeployables, deployablesDestination );
            }
            else {
                FileUtils.write( deployablesDestination, deployablesMergeHelper.objectToJson( deployables ), "UTF-8" );
            }

            if ( isCopyAggregatedArtifacts ) {
                for ( DeployDetails details : deployables ) {
                    /**
                     * We could check MD5 checksum of destination file (if it exists) and save on copy operation but since most *.jar
                     * files contain a timestamp in pom.properties (thanks, Maven) - checksum would only match for POM files.
                     */
                    File aggregatedFile = aggregatedFile( aggregateDirectory, details.getFile());
                    FileUtils.copyFile( details.getFile(), aggregatedFile );
                }
            }

            return ( isPublishAggregatedArtifacts && ( mergedDeployables != null )) ?
                       convertDeployables( aggregateDirectory, mergedDeployables, isCopyAggregatedArtifacts ) :
                       deployables;
        }
        catch ( IOException e ){
            throw new RuntimeException( "Failed to aggregate artifacts and Build Info in [" + aggregateDirectory + "]",
                                        e );
        }
    }


    @SuppressWarnings({ "FeatureEnvy" , "SuppressionAnnotation" })
    private Set<DeployDetails> convertDeployables ( File aggregateDirectory, Iterable<Map<String, ?>> deployables, boolean isCopyAggregatedArtifacts )
        throws IOException
    {
        Set<DeployDetails> result = new HashSet<DeployDetails>();

        for ( Map<String,?> map : deployables ) {

            File file = new File(( String ) map.get( "file" ));
            if ( isCopyAggregatedArtifacts ){ file = aggregatedFile( aggregateDirectory, file ); }

            DeployDetails.Builder builder = new DeployDetails.Builder().
                                            targetRepository(( String ) map.get( "targetRepository" )).
                                            artifactPath(( String ) map.get( "artifactPath" )).
                                            file( file ).
                                            sha1(( String ) map.get( "sha1" )).
                                            md5(( String ) map.get( "md5" )).
                                            addProperties(( Map<String, String> ) map.get( "properties" ));
            result.add( builder.build());
        }

        return result;
    }


    private File aggregatedFile( File aggregateDirectory, File file ) throws IOException
    {
        String workspacePath        = aggregateDirectory.getParentFile().getCanonicalPath().replace( '\\', '/' );
        String artifactPath         = file.getCanonicalPath().replace( '\\', '/' );
        String artifactRelativePath = artifactPath.startsWith( workspacePath ) ?
           /**
            * "/Users/evgenyg/.hudson/jobs/teamcity-artifactory-plugin/workspace/agent/target/teamcity-artifactory-plugin-agent-2.1.x-SNAPSHOT.jar" =>
            * "agent/target/teamcity-artifactory-plugin-agent-2.1.x-SNAPSHOT.jar"
            */
            artifactPath.substring( workspacePath.length() + 1 ) :
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
            if(artifacts!=null){
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
        }
        return deployableArtifacts;
    }

    private void deployArtifacts(ArtifactoryClientConfiguration clientConf, Set<DeployDetails> deployableArtifacts) {
        ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf);
        try {
            IncludeExcludePatterns includeExcludePatterns = getArtifactDeploymentPatterns(clientConf.publisher);
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
        } finally {
            client.close();
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
