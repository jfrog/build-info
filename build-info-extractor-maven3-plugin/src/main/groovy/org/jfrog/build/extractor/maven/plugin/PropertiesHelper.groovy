package org.jfrog.build.extractor.maven.plugin

import static org.jfrog.build.extractor.maven.plugin.Utils.*
import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires
import org.jfrog.build.api.BuildInfoConfigProperties


/**
 * Helper merging all mojo properties.
 */
class PropertiesHelper
{
    @Delegate
    private final ExtractorMojo mojo

    @SuppressWarnings([ 'GrFinalVariableAccess' ])
    @Requires({ mojo })
    @Ensures ({ this.mojo })
    PropertiesHelper ( ExtractorMojo mojo )
    {
        this.mojo = mojo
    }


   /**
    * Merges *.properties files with <configuration> values and writes a new *.properties file to be picked up later
    * by the original Maven listener.
    */
    @Ensures ({ result })
    Properties mergeProperties ()
    {
        final systemProperty     = System.getProperty( BuildInfoConfigProperties.PROP_PROPS_FILE )
        final systemProperties   = readProperties( systemProperty ? new File( systemProperty ) : '' )

        Properties pomProperties = readProperties( mojo.propertiesFile )
        pomProperties           += readProperties( mojo.properties )

        final propertiesFile     = writeProperties ? new File( mojo.project.basedir, 'buildInfo.properties' ) :
                                                     File.createTempFile( 'buildInfo', '.properties' )

        buildInfoConfigPropertiesFile = propertiesFile.canonicalPath
        final  mergedProperties       = mergePropertiesWithFields( systemProperties, pomProperties )
        assert mergedProperties

        assert propertiesFile.parentFile.with { File f -> f.directory || f.mkdirs() }
        propertiesFile.withWriter { Writer w -> mergedProperties.store( w, 'Build Info Properties' )}
        if ( ! writeProperties ){ propertiesFile.deleteOnExit() }

        log.info( "Merged properties file:$propertiesFile.canonicalPath created" )
        System.setProperty( BuildInfoConfigProperties.PROP_PROPS_FILE, propertiesFile.canonicalPath )

        mergedProperties
    }


    /**
     * Merges system-provided properties with POM properties and class fields.
     */
    @Requires({ ( systemProperties != null ) && ( pomProperties != null ) })
    @Ensures ({ result != null })
    private Properties mergePropertiesWithFields ( Properties systemProperties, Properties pomProperties )
    {
        final  mergedProperties = ( Properties ) ( pomPropertiesPriority ? systemProperties + pomProperties : pomProperties + systemProperties )
        final  propertyFields   = ExtractorMojoProperties.declaredFields.findAll{ ( it.name != 'deployProperties' ) && it.getAnnotation( Property ) }
        assert propertyFields

        for ( field in propertyFields )
        {
            assert field.name && ( field.type == String )

            final  property     = field.getAnnotation( Property )
            final  propertyName = property.name()
            assert propertyName

            if (( ! systemProperties[ propertyName ] ) || pomPropertiesPriority )
            {
                final  propertyValue = mojo."${ field.name }" ?: mergedProperties[ propertyName ] ?: property.defaultValue()
                assert ( propertyValue == null ) || ( propertyValue instanceof String )
                if ( propertyValue )
                {
                    mergedProperties[ propertyName ] = propertyValue
                }
            }
            else
            {
                assert systemProperties[ propertyName ] && ( ! pomPropertiesPriority ) &&
                       ( systemProperties[ propertyName ] == mergedProperties[ propertyName ])
            }
        }

        if ( deployProperties ){ addDeployProperties( mergedProperties, systemProperties )}
        addHandlersProperties( mergedProperties, systemProperties )

        ( Properties ) mergedProperties.collectEntries { String key, String value -> [ key, updateValue( value ) ]}
    }


    /**
     * Adds deployment properties to properties merged.
     */
    @Requires({ ( properties != null ) && ( systemProperties != null ) && deployProperties })
    private void addDeployProperties( Properties properties, Properties systemProperties )
    {
        final deployPropertiesBase = propertyName( 'deployProperties' )
        readProperties( deployProperties ).each {
            String key, String value -> addProperty( "$deployPropertiesBase.$key", value, properties, systemProperties )
        }
    }


    /**
     * Adds handler properties to properties merged.
     */
    @SuppressWarnings([ 'GroovyAccessibility' ])
    @Requires({ ( properties != null ) && ( systemProperties != null ) &&
                ( resolver.delegate && publisher.delegate && buildInfo.delegate ) })
    private void addHandlersProperties ( Properties properties, Properties systemProperties )
    {
        assert ( resolver.delegate.props.is( publisher.delegate.props )) && ( publisher.delegate.props.is( buildInfo.delegate.props ))
        resolver.delegate.props.each { String key, String value -> addProperty( key, value, properties, systemProperties )}
    }


    /**
     * Adds property provided to {@link Properties} instance based on {@link ExtractorMojo#pomPropertiesPriority}
     * and system properties specified.
     */
    @Requires({ propertyName && ( propertyValue != null ) && ( properties != null ) && ( systemProperties != null ) })
    @Ensures ({ properties[ propertyName ] != null })
    private void addProperty( String propertyName, String propertyValue, Properties properties, Properties systemProperties )
    {
        properties[ propertyName ] = ( systemProperties[ propertyName ] && ( ! pomPropertiesPriority )) ?
            systemProperties[ propertyName ] :
            propertyValue
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
}
