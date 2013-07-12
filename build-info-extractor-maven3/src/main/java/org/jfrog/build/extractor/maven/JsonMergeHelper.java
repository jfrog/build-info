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
            throw new RuntimeException( String.format( "Failed to convert object '%s' to JSON", o ), e );
        }
    }


    <T> T jsonToObject ( String jsonContent, Class<T> type ) {
        try {
            return new ObjectMapper().reader( type ).readValue( jsonContent );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to convert JSON '%s' to object", jsonContent ), e );
        }
    }


    <T> T jsonToObject ( File jsonFile, Class<T> type ) {
        try {
            return new ObjectMapper().reader( type ).readValue( jsonFile );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to convert JSON file '%s' to object", jsonFile ), e );
        }
    }


    Map<String, ?> mergeAndWrite ( Map<String, ?> source, Map<String, ?> destination, File destinationFile )
    {
        return jsonWrite( mergeMaps( source, destination ), destinationFile );
    }


    <T> List<T> mergeAndWrite ( List<T> source, List<T> destination, File destinationFile )
    {
        return jsonWrite( mergeLists( source, destination ), destinationFile );
    }


    String mergeJsons( String source, String destination ) {
        Object sourceObject      = jsonToObject( source, Object.class );
        Object destinationObject = jsonToObject( destination, Object.class );
        Object mergedObject      =
            (( sourceObject instanceof Map  ) && ( destinationObject instanceof Map  )) ? mergeMaps(( Map ) sourceObject, ( Map ) destinationObject ) :
            (( sourceObject instanceof List ) && ( destinationObject instanceof List )) ? mergeLists( ( List ) sourceObject, ( List ) destinationObject ) :
                                                                                          null;
        if ( mergedObject == null ) {
            throw new RuntimeException( String.format( "Unable to merge JSON content of '%s' and '%s'", source, destination ));
        }

        return objectToJson( mergedObject );
    }


    Map<String,?> mergeMaps ( Map<String, ?> source, Map<String, ?> destination ){

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


    <T> List<T> mergeLists ( List<T> source, List<T> destination )
    {
        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        if ( source.get( 0 ) instanceof Map ) {
            Map map = ( Map ) source.get( 0 );

            for ( String mapIdentifier : mapIdentifiers )
            {
                if ( map.get( mapIdentifier ) != null ) {
                    return ( List<T> ) mergeListsOfMaps(( List<Map<String, ?>> ) source, ( List<Map<String, ?>> ) destination, mapIdentifier );
                }
            }
        }

        List<T> result = new ArrayList<T>( destination );
        result.addAll( source );
        return new ArrayList<T>( new HashSet<T>( result ));
    }


    private <T> T jsonWrite ( T object, File destinationFile )
    {
        try {
            FileUtils.write( destinationFile, objectToJson( object ), "UTF-8" );
            return object;
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to write [%s] to [%s]", object, destinationFile ), e );
        }
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
