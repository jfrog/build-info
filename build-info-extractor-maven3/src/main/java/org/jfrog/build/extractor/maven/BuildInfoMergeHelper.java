package org.jfrog.build.extractor.maven;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.util.*;


/**
 * Helper class for merging tow Build Info JSON data.
 */
class BuildInfoMergeHelper
{

    Map<String,Object> fileToJsonMap ( File jsonFile ) {
        try {
            return ( Map<String,Object> ) new ObjectMapper().readValue( jsonFile, Map.class );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to read JSON file '%s'", jsonFile ), e );
        }
    }


    String jsonMapToString ( Map<String, ?> jsonMap ) {
        try {
            return new ObjectMapper().writer().withDefaultPrettyPrinter().writeValueAsString( jsonMap );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to convert map %s to JSON", jsonMap ), e );
        }
    }


    void mergeAndWriteBuildInfoMaps ( Map<String, ?> buildInfoSourceMap, Map<String, ?> buildInfoDestinationMap, File buildInfoDestination )
    {
        try {
            Map<String,?> buildInfoMergedMap = mergeBuildInfoMaps( buildInfoSourceMap, buildInfoDestinationMap );
            FileUtils.write( buildInfoDestination, jsonMapToString( buildInfoMergedMap ), "UTF-8" );
        }
        catch ( Exception e ) {
            throw new RuntimeException( String.format( "Failed to write merged Build Info to '%s'", buildInfoDestination ), e );
        }
    }


    private Map<String,?> mergeBuildInfoMaps ( Map<String,?> source, Map<String,?> destination ){

        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        Map<String,Object> result = new HashMap<String,Object>( destination );

        for ( Map.Entry<String,?> entry : source.entrySet()) {
            String sourceKey   = entry.getKey();
            Object sourceValue = entry.getValue();
            Object mergedValue = ( sourceValue instanceof List ) ? mergeBuildInfoLists(( List ) sourceValue, ( List ) result.get( sourceKey )) :
                                 ( sourceValue instanceof Map  ) ? mergeBuildInfoMaps(( Map ) sourceValue, ( Map ) result.get( sourceKey ))    :
                                                                   sourceValue;
            result.put( sourceKey, mergedValue );
        }

        return result;
    }


    private List<?> mergeBuildInfoLists ( List<?> source, List<?> destination )
    {
        if (( source      == null ) || source.isEmpty()){ return destination; }
        if (( destination == null ) || destination.isEmpty()){ return source; }
        if ( source.equals( destination )){ return source; }

        if ( source.get( 0 ) instanceof Map ) {
            Map map = ( Map ) source.get( 0 );
            if ( map.get( "id" ) != null ) {
                return mergeBuildInfoListsOfMaps (( List<Map<String,?>> ) source, ( List<Map<String,?>> ) destination, "id" );
            }
            else if ( map.get( "name" ) != null ) {
                return mergeBuildInfoListsOfMaps (( List<Map<String,?>> ) source, ( List<Map<String,?>> ) destination, "name" );
            }
        }

        List result = new ArrayList<Object>( destination );
        result.addAll( source );
        return new ArrayList<Object>( new HashSet<Object>( result ));
    }


    private List<Map<String,?>> mergeBuildInfoListsOfMaps ( List<Map<String,?>> source, List<Map<String,?>> destination, String mapIdentifier )
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
                        result.add( mergeBuildInfoMaps( sourceMap, destinationMap ));
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
