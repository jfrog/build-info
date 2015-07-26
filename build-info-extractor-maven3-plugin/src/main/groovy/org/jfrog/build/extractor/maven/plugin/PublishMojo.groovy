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
import org.codehaus.gmaven.mojo.GroovyMojo
import org.gcontracts.annotations.Requires
import org.jfrog.build.api.BuildInfoProperties
import org.jfrog.build.extractor.maven.BuildInfoRecorder
import org.jfrog.build.extractor.maven.BuildInfoRecorderLifecycleParticipant

import java.text.SimpleDateFormat

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@Mojo ( name = 'publish', defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true )
class PublishMojo extends GroovyMojo
{
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
    boolean pomPropertiesPriority = false

    @Parameter
    Map<String, String> deployProperties = [:]

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

    /**
     * Helper object
     */
    private PublishMojoHelper helper


    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ session && log })
    @Override
    void execute ()
         throws MojoExecutionException , MojoFailureException
    {
        boolean invokedAlready = ( session.request.executionListener instanceof BuildInfoRecorder )

        if ( invokedAlready ){ return }

        helper = new PublishMojoHelper( this )
        if ( log.debugEnabled ){ helper.printConfigurations() }

        skipDefaultDeploy()
        completeConfig()
        helper.createPropertiesFile()
        recordBuildInfo()
    }


    /**
     * Cancels default "deploy" activity.
     */
    private void skipDefaultDeploy ()
    {
        // For Maven versions < 3.3.3:
        session.executionProperties[ 'maven.deploy.skip' ] = Boolean.TRUE.toString()
        // For Maven versions >= 3.3.3:
        session.getUserProperties().put("maven.deploy.skip", Boolean.TRUE.toString())
    }


    /**
     * Completes various configuration settings.
     */
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ buildInfo && artifactory && session && project })
    private void completeConfig ()
    {
        final format                = { Date d  -> new SimpleDateFormat( 'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ' ).format( d ) } // 2013-06-23T18\:38\:37.597+0200
        buildInfo.buildTimestamp    = session.startTime.time as String
        buildInfo.buildStarted      = format( session.startTime )
        buildInfo.buildName         = helper.updateValue( buildInfo.buildName   ) ?: project.artifactId
        buildInfo.buildNumber       = helper.updateValue( buildInfo.buildNumber ) ?: buildInfo.buildTimestamp
        buildInfo.buildAgentName    = 'Maven'
        buildInfo.buildAgentVersion = helper.mavenVersion()
        blackDuck.runChecks         = blackDuck.delegate.props.keySet().any { it.startsWith( BuildInfoProperties.BUILD_INFO_BLACK_DUCK_PROPERTIES_PREFIX )}

        if ( buildInfo.buildRetentionDays != null ){
            buildInfo.buildRetentionMinimumDate = buildInfo.buildRetentionDays as String
        }

        artifactory.activateRecorder = true
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
