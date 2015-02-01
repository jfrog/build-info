package org.jfrog.build.extractor.maven.plugin

import org.apache.maven.Maven
import org.jfrog.build.api.BuildInfoFields
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.jfrog.build.api.BuildInfoConfigProperties
import org.jfrog.build.extractor.clientConfiguration.ClientProperties
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * Helper merging all mojo properties.
 */
class PublishMojoHelper
{
    @Delegate
    private final PublishMojo mojo

    private final Properties systemProperties

    /**
     * Mapping of mojo parameters of type {@link Config.DelegatesToPrefixPropertyHandler}: name => value.
     */
    private final Map<String, Config.DelegatesToPrefixPropertyHandler> prefixPropertyHandlers

    /**
     * Mapping of types printed by {@link #printConfigurations()}: class => description.
     */
    private final static Map<Class<?>, String> TYPE_DESCRIPTORS = [ ( Boolean ) : 'true/false',
                                                                    ( boolean ) : 'true/false',
                                                                    ( Integer ) : 'N',
                                                                    ( Long    ) : 'N',
                                                                    ( File    ) : 'path/to/file',
                                                                    ( String  ) : ' .. ' ].asImmutable()

    @Requires({ mojo })
    @Ensures ({ this.mojo && ( this.systemProperties != null ) && prefixPropertyHandlers })
    PublishMojoHelper ( PublishMojo mojo )
    {
        this.mojo              = mojo
        final systemProperty   = System.getProperty( BuildInfoConfigProperties.PROP_PROPS_FILE )
        this.systemProperties  = readProperties( systemProperty ? new File( systemProperty ) : '' )
        prefixPropertyHandlers = (( Map ) mojo.class.declaredFields.
                                 findAll{ Field f -> Config.DelegatesToPrefixPropertyHandler.isAssignableFrom( f.type ) }.
                                 inject( [:] ) { Map m, Field f -> m[ f.name ] = mojo."${ f.name }"; m }).
                                 asImmutable()
    }


    /**
     * Retrieves current Maven version.
     */
    @Ensures ({ result })
    String mavenVersion()
    {
        final  resourceLocation = 'META-INF/maven/org.apache.maven/maven-core/pom.properties'
        final  resourceStream   = Maven.classLoader.getResourceAsStream( resourceLocation )
        assert resourceStream, "Failed to load '$resourceLocation'"

        final properties = new Properties()
        properties.load( resourceStream )
        resourceStream.close()

        properties[ 'version' ] ?: 'Unknown'
    }


    /**
     * Prints out all possible Mojo <configuration> settings.
     */
    @Requires({ log.debugEnabled && artifactory && prefixPropertyHandlers })
    void printConfigurations ()
    {
        final Map<String, Object> objectsMap = [ '' : this, artifactory : artifactory ] + prefixPropertyHandlers
        final List<String>        lines      = [ 'Possible <configuration> values:' ]   + objectsMap.collect {
            String objectName, Object object ->
            objectName ? [ "<$objectName>", objectConfigurations( object ).collect { "  $it" }, "</$objectName>" ] :
                         objectConfigurations( object )
        }.flatten()

        log.debug( lines.join( '\n' ))
    }


    /**
     * Retrieves a list of all object settings.
     */
    @Requires({ object != null })
    @Ensures ({ result })
    private List<String> objectConfigurations ( Object object )
    {
        object.class.methods.findAll { Method m -> ( m.name.length() > 3 )          &&
                                                   ( m.name.startsWith( 'set' ))    &&
                                                   ( m.parameterTypes.length == 1 ) &&
                                                   TYPE_DESCRIPTORS.keySet().any { it.isAssignableFrom( m.parameterTypes.first()) }}.
                              collect { Method m ->
                                  final tag = "${ m.name.charAt( 3 ).toLowerCase()}${ m.name.substring( 4 )}"
                                  "<$tag>${ TYPE_DESCRIPTORS[ m.parameterTypes.first()] }</$tag>"
                              }.
                              sort()
    }


   /**
    * Merges *.properties files with <configuration> values and writes a new *.properties file to be picked up later
    * by the original Maven listener.
    */
    @Ensures ({ result })
    Properties createPropertiesFile ()
    {
        final  mergedProperties = mergeProperties()
        assert mergedProperties

        final propertiesFile = artifactory.propertiesFile ? new File( mojo.project.basedir, artifactory.propertiesFile ) :
                                                            File.createTempFile( 'buildInfo', '.properties' )

        mergedProperties[ BuildInfoConfigProperties.PROP_PROPS_FILE ] = propertiesFile.canonicalPath
        propertiesFile.withWriter { Writer w -> mergedProperties.store( w, 'Build Info Properties' )}
        if ( ! artifactory.propertiesFile ){ propertiesFile.deleteOnExit() }

        log.info( "Merged properties file:${ propertiesFile.canonicalPath } created" )
        System.setProperty( BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFile.canonicalPath )

        mergedProperties
    }


    /**
     * Merges system-provided properties with POM properties and class fields.
     */
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ prefixPropertyHandlers && artifactory.delegate.root.props && buildInfo.buildTimestamp && buildInfo.buildName })
    @Ensures ({ result != null })
    private Properties mergeProperties ()
    {
        assert prefixPropertyHandlers.values().each { assert it.delegate?.props && it.delegate.props.is( artifactory.delegate.root.props ) }

        final mergedProperties = new Properties()
        final deployProperties = ([ ( BuildInfoFields.BUILD_TIMESTAMP ) : buildInfo.buildTimestamp,
                                    ( BuildInfoFields.BUILD_NAME      ) : buildInfo.buildName,
                                    ( BuildInfoFields.BUILD_NUMBER    ) : buildInfo.buildNumber ] +
                                  deployProperties ).collectEntries {
            String key, String value -> [ "${ ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX }${ key }".toString(), value ]
        }

        addProperties(( Map<String, String> ) readProperties( mojo.propertiesFile ) + readProperties( mojo.properties ),
                      mergedProperties )

        addProperties( artifactory.delegate.root.props,
                       mergedProperties )

        addProperties(( Map<String, String> ) deployProperties,
                      mergedProperties )

        ( Properties ) mergedProperties.collectEntries {
            String key, String value ->

            final valueUpdated = ( value != null ) ? updateValue( value ) : null
            ( valueUpdated != null ) ? [ ( key ) : valueUpdated ] : [ : ]
        }
    }


    /**
     * Adds all {@code propertiesFrom} to {@code propertiesTo} considering {@link #systemProperties}.
     */
    @SuppressWarnings([ 'GroovyAssignmentToMethodParameter' ])
    @Requires({ ( propertiesFrom != null ) && ( propertiesTo != null ) && ( systemProperties != null ) })
    private void addProperties ( Map<String, String> propertiesFrom, Properties propertiesTo )
    {
        propertiesFrom.each {
            String propertyName, String propertyValue ->
            propertyName   = propertyName.toString() // Possible GString => String
            final newValue = (( propertyValue == null ) || ( systemProperties[ propertyName ] && ( ! pomPropertiesPriority ))) ?
                                systemProperties[ propertyName ] :
                                propertyValue
            if ( newValue != null ){ propertiesTo[ propertyName ] = newValue }
        }
    }


    /**
     * Reads {@link Properties} from the {@link File} specified.
     */
    @Ensures ({ result != null })
    private Properties readProperties ( File propertiesFile )
    {
        assert (( ! propertiesFile ) || propertiesFile.file ), "Properties file [$propertiesFile.canonicalPath] is not available"
        readProperties( propertiesFile?.file ? propertiesFile.getText( 'UTF-8' ) : '' )
    }


    /**
     * Reads {@link Properties} from the {@link String} specified.
     */
    @Ensures ({ result != null })
    private Properties readProperties ( String propertiesContent )
    {
        final p = new Properties()
        if ( propertiesContent ){ p.load( new StringReader( propertiesContent )) }
        p
    }


    /**
     * Updates all "{{var1|var2|var3}}" entries in the value specified to their corresponding environment variables
     * or system properties. Last variable is the fallback (default) value if wrapped in double quotes.
     * See PublishMojoHelperSpec.
     */
    String updateValue( String value )
    {
        if ( ! value?.with{ contains( '{{' ) && contains( '}}' ) }){ return value?.trim() }

        final   isQuoted    = { String s -> s?.with { startsWith( '"' ) && endsWith( '"' ) }}
        final   unquote     = { String s -> s.substring( 1, s.size() - 1 )}
        boolean defaultUsed = false
        final   result      = value.trim().replaceAll( /\{\{([^}]*)\}\}/ ){

            final expressions  = (( String ) it[ 1 ] ).tokenize( '|' )*.trim().grep()

            if ( ! expressions ){ return null }

            final lastValue    = expressions[ -1 ]
            final defaultValue = isQuoted( lastValue )    ? unquote( lastValue ) : null
            defaultUsed        = ( defaultValue != null ) || defaultUsed
            final variables    = ( defaultValue != null ) ? (( expressions.size() > 1 ) ? expressions[ 0 .. -2 ] : [] ) :
                                                            expressions

            variables.collect { System.getenv( it ) ?: System.getProperty( it )}.grep()[ 0 ] ?: defaultValue
        }

        ( result != 'null' ) ? result.replace( 'null', '' ) :
        ( defaultUsed      ) ? '' :
                               null
    }
}
