package org.jfrog.build.extractor.maven;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.util.*;


/**
 * Helper class for merging JSON data.
 */
class JsonMergeHelper
{
    String[] mapIdentifiers;

    JsonMergeHelper ( String ... mapIdentifiers )
    {
        this.mapIdentifiers = mapIdentifiers;
    }


    String objectToJson ( Object o ) {
        try {
            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString( o );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to convert object [%s] to JSON", o ), e );
        }
    }


    <T> T jsonToObject ( File jsonFile, Class<T> type ) {
        try {
            return new ObjectMapper().reader( type ).readValue( jsonFile );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to read JSON file '%s'", jsonFile ), e );
        }
    }


    void mergeAndWrite ( Map<String, ?> source, Map<String, ?> destination, File destinationFile )
    {
        jsonWrite( mergeMaps( source, destination ), destinationFile );
    }


    void mergeAndWrite ( List<?> source, List<?> destination, File destinationFile )
    {
        jsonWrite( mergeLists( source, destination ), destinationFile );
    }


    void jsonWrite ( Object o, File destinationFile )
    {
        try {
            FileUtils.write( destinationFile, objectToJson( o ), "UTF-8" );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to write [%s] to [%s]", o, destinationFile ), e );
        }
    }


    private Map<String,?> mergeMaps ( Map<String, ?> source, Map<String, ?> destination ){

        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        Map<String,Object> result = new HashMap<String,Object>( destination );

        for ( Map.Entry<String,?> entry : source.entrySet()) {
            String sourceKey   = entry.getKey();
            Object sourceValue = entry.getValue();
            Object mergedValue = ( sourceValue instanceof List ) ? mergeLists(( List ) sourceValue, ( List ) result.get( sourceKey )) :
                                 ( sourceValue instanceof Map  ) ? mergeMaps(( Map ) sourceValue, ( Map ) result.get( sourceKey ))    :
                                                                   sourceValue;
            result.put( sourceKey, mergedValue );
        }

        return result;
    }


    private List<?> mergeLists ( List<?> source, List<?> destination )
    {
        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        if ( source.get( 0 ) instanceof Map ) {
            Map map = ( Map ) source.get( 0 );

            for ( String mapIdentifier : mapIdentifiers )
            {
                if ( map.get( mapIdentifier ) != null ) {
                    return mergeListsOfMaps(( List<Map<String, ?>> ) source, ( List<Map<String, ?>> ) destination, mapIdentifier );
                }
            }
        }

        List result = new ArrayList<Object>( destination );
        result.addAll( source );
        return new ArrayList<Object>( new HashSet<Object>( result ));
    }


    private List<Map<String,?>> mergeListsOfMaps ( List<Map<String, ?>> source, List<Map<String, ?>> destination, String mapIdentifier )
    {
        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        List<Map<String,?>> result = new ArrayList<Map<String, ?>>( Math.max( source.size(), destination.size()));

        for ( Map<String,?> sourceMap: new ArrayList<Map<String, ?>>( source )) {

            boolean matchingMapFound = false;
            String  sourceMapId      = ( String ) sourceMap.get( mapIdentifier );

            for ( Map<String,?> destinationMap: new ArrayList<Map<String, ?>>( destination )) {
                if ( ! matchingMapFound ){
                    String destinationMapId = ( String ) destinationMap.get( mapIdentifier );
                    if ( sourceMapId.equals( destinationMapId )) {
                        matchingMapFound = true;
                        result.add( mergeMaps( sourceMap, destinationMap ));
                        source.remove( sourceMap );
                        destination.remove( destinationMap );
                    }
                }
            }
        }

        result.addAll( destination );
        result.addAll( source );

        return result;
    }
}
