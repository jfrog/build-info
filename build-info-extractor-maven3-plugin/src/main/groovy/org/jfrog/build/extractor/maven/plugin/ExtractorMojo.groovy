package org.jfrog.build.extractor.maven.plugin

import org.apache.maven.AbstractMavenLifecycleParticipant
import org.apache.maven.execution.ExecutionEvent
import org.apache.maven.execution.MavenSession
import org.apache.maven.lifecycle.internal.DefaultExecutionEventCatapult
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.apache.maven.repository.internal.DefaultArtifactDescriptorReader
import org.codehaus.gmaven.mojo.GroovyMojo
import org.gcontracts.annotations.Requires
import org.jfrog.build.extractor.maven.BuildInfoRecorder
import org.jfrog.build.extractor.maven.BuildInfoRecorderLifecycleParticipant
import org.sonatype.aether.RepositorySystem
import org.sonatype.aether.impl.ArtifactDescriptorReader
import org.sonatype.aether.impl.internal.DefaultRepositorySystem
import java.text.SimpleDateFormat


/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@Mojo ( name = 'extract-build-info', defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
class ExtractorMojo extends GroovyMojo
{
    private final helper = new ExtractorMojoHelper( this )

    /**
     * ---------------------------
     * Container-injected objects
     * ---------------------------
     */

    @Parameter ( required = true, defaultValue = '${project}' )
    private MavenProject project

    @Parameter ( required = true, defaultValue = '${session}' )
    private MavenSession session

    @Component( role = AbstractMavenLifecycleParticipant )
    private BuildInfoRecorderLifecycleParticipant listener

    @Component( role = ExecutionEventCatapult )
    private DefaultExecutionEventCatapult eventCatapult

    @Component( role = RepositorySystem )
    DefaultRepositorySystem repoSystem

    @Component( role = ArtifactDescriptorReader )
    DefaultArtifactDescriptorReader descriptorReader

    /**
     * ----------------
     * Mojo parameters
     * ----------------
     */

    @Parameter
    File propertiesFile

    @Parameter
    String properties

    @Parameter
    String deployGoals = 'deploy,maven-deploy-plugin'

    @Parameter
    boolean writeProperties = false

    @Parameter
    boolean pomPropertiesPriority = false

    @Parameter
    String deployProperties

    /**
     * ----------------
     * Mojo parameters - property handlers
     * ----------------
     */

    @Parameter
    Config.Artifactory artifactory = new Config.Artifactory()

    @Parameter
    Config.Resolver resolver  = new Config.Resolver()

    @Parameter
    Config.Publisher publisher = new Config.Publisher()

    @Parameter
    Config.BuildInfo buildInfo = new Config.BuildInfo()

    @Parameter
    Config.LicenseControl licenses  = new Config.LicenseControl()

    @Parameter
    Config.IssuesTracker issues = new Config.IssuesTracker()

    @Parameter
    Config.BlackDuck blackDuck = new Config.BlackDuck()


    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ descriptorReader && repoSystem && session && log })
    @Override
    void execute ()
         throws MojoExecutionException , MojoFailureException
    {
        boolean invokedAlready = (( descriptorReader.artifactResolver instanceof RepositoryResolver ) ||
                                  ( repoSystem.artifactResolver       instanceof RepositoryResolver ) ||
                                  ( session.request.executionListener instanceof BuildInfoRecorder  ))

        if ( invokedAlready   ){ return }
        if ( log.debugEnabled ){ helper.printConfigurations() }

        skipDefaultDeploy()
        updateBuildInfo()
        helper.createPropertiesFile()
        overrideResolutionRepository()
        recordBuildInfo()
    }


    /**
     * Cancels default "deploy" activity.
     */
    private void skipDefaultDeploy ()
    {
        session.executionProperties[ 'maven.deploy.skip' ] = 'true'
    }


    /**
     * Updates {@link #buildInfo} fields.
     */
    @Requires({ buildInfo && session && project })
    private void updateBuildInfo ()
    {
        buildInfo.buildTimestamp = session.startTime.time as String
        buildInfo.buildStarted   = new SimpleDateFormat( 'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ' ).format( session.startTime ) // 2013-06-23T18\:38\:37.597+0200
        buildInfo.buildName      = helper.updateValue( buildInfo.buildName   ) ?: project.artifactId
        buildInfo.buildNumber    = helper.updateValue( buildInfo.buildNumber ) ?: buildInfo.buildTimestamp
    }


    /**
     * Overrides resolution repository if a corresponding property is set.
     */
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ resolver && descriptorReader.artifactResolver && repoSystem.artifactResolver })
    private void overrideResolutionRepository ()
    {
        final String artifactoryUrl = helper.updateValue( resolver.contextUrl )
        final String resolutionRepo = helper.updateValue( resolver.repoKey )

        if ( artifactoryUrl && resolutionRepo )
        {
            final remoteRepository            = "$artifactoryUrl${ artifactoryUrl.endsWith( '/' ) || resolutionRepo.startsWith( '/' ) ? '' : '/' }$resolutionRepo"
            descriptorReader.artifactResolver = new RepositoryResolver( descriptorReader.artifactResolver, remoteRepository )
            repoSystem.artifactResolver       = new RepositoryResolver( repoSystem.artifactResolver,       remoteRepository )
            log.info( "Remote resolution repository set to [$remoteRepository]" )
        }
    }


    /**
     * Adds original Maven listener to the lifecycle so that it records and publishes build info, together with build artifacts.
     */
    @Requires({ session && deployGoals && session.goals && listener && eventCatapult })
    private void recordBuildInfo()
    {
        final isDeployGoal = deployGoals.tokenize( ',' )*.trim().any {
            String deployGoal -> session.goals.any {
            String goal      -> (( goal == deployGoal ) || goal.contains( deployGoal ))
            }
        }

        if ( isDeployGoal )
        {
            listener.afterSessionStart( session )
            listener.afterProjectsRead( session )
            eventCatapult.fire( ExecutionEvent.Type.SessionStarted, session, null )
            eventCatapult.fire( ExecutionEvent.Type.ProjectStarted, session, null )
        }
    }
}
