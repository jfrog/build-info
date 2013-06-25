package org.jfrog.build.extractor.maven.plugin

import org.gcontracts.annotations.Ensures
import org.gcontracts.annotations.Requires


/**
 * Various helper methods.
 */
final class Utils
{
    private Utils (){}


    /**
     * Updates all "${var}" entries in the value specified to their corresponding environment variables or system properties.
     */
    @Requires({ value })
    @Ensures ({ result })
    static String updateValue( String value )
    {
        value?.replaceAll( /\$\{([^}]+)\}/ ){
            final String var = it[ 1 ]
            System.getenv( var ) ?: System.getProperty( var ) ?: '${' + var + '}'
        }
    }


    /**
     * Retrieves mojo field's property name reading its {@link Property} annotation.
     */
    @Requires({ fieldName })
    @Ensures ({ result })
    static String propertyName ( String fieldName )
    {
        ExtractorMojoProperties.getDeclaredField( fieldName ).getAnnotation( Property ).name()
    }
}
