package org.jfrog.build.extractor.maven.plugin

import org.jfrog.build.api.BuildInfoFields
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.jfrog.build.api.BuildInfoConfigProperties
import org.jfrog.build.client.ClientProperties
import java.lang.reflect.Field
import java.lang.reflect.Method


/**
 * Helper merging all mojo properties.
 */
class ExtractorMojoHelper
{
    @Delegate
    private final ExtractorMojo mojo

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
                                                                    ( Number  ) : 'N',
                                                                    ( File    ) : 'path/to/file',
                                                                    ( String  ) : ' .. ' ].asImmutable()


    @SuppressWarnings([ 'GrFinalVariableAccess' ])
    @Requires({ mojo })
    @Ensures ({ this.mojo && ( this.systemProperties != null ) && prefixPropertyHandlers })
    ExtractorMojoHelper ( ExtractorMojo mojo )
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

        final propertiesFile = writeProperties ? new File( mojo.project.basedir, 'buildInfo.properties' ) :
                                                 File.createTempFile( 'buildInfo', '.properties' )

        mergedProperties[ BuildInfoConfigProperties.PROP_PROPS_FILE ] = propertiesFile.canonicalPath
        propertiesFile.withWriter { Writer w -> mergedProperties.store( w, 'Build Info Properties' )}
        if ( ! writeProperties ){ propertiesFile.deleteOnExit() }

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
        assert prefixPropertyHandlers.values().each { assert it.delegate.props.is( artifactory.delegate.root.props ) }

        final mergedProperties       = new Properties()
        final deployPropertiesPrefix = ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX
        final deployProperties       = (( Properties ) readProperties( deployProperties ).collectEntries {
            String key, String value -> [ "${ deployPropertiesPrefix }${ key }", value ]
        }) + [ ( "${ deployPropertiesPrefix }${ BuildInfoFields.BUILD_TIMESTAMP }".toString()) : buildInfo.buildTimestamp,
               ( "${ deployPropertiesPrefix }${ BuildInfoFields.BUILD_NAME }".toString())      : buildInfo.buildName ]

        addProperties(( Map<String, String> ) readProperties( mojo.propertiesFile ) + readProperties( mojo.properties ),
                      mergedProperties )

        addProperties( artifactory.delegate.root.props,
                       mergedProperties )

        addProperties(( Map<String, String> ) deployProperties,
                      mergedProperties )

        ( Properties ) mergedProperties.collectEntries { String key, String value -> [ key, updateValue( value ) ]}
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
            propertyName = propertyName.toString() // Possible GString => String
            propertiesTo[ propertyName ] = ( systemProperties[ propertyName ] && ( ! pomPropertiesPriority )) ?
                systemProperties[ propertyName ] :
                propertyValue
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
     * Updates all "${var}" entries in the value specified to their corresponding environment variables or system properties.
     */
    @Requires({ value })
    @Ensures ({ result })
    private String updateValue( String value )
    {
        value?.replaceAll( /(\$?\{)([^}]+)(\})/ ){
            final expressionValue = (( String ) it[ 2 ] ).tokenize( '|' ).collect { System.getenv( it ) ?: System.getProperty( it )}.grep()[ 0 ]
            expressionValue ?: "${ it[ 1 ] }${ it[ 2 ] }${ it[ 3 ] }"
        }
    }
}
