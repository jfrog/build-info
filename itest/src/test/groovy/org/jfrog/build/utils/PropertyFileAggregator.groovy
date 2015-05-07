package org.jfrog.build.utils

/**
 * @author Aviad Shikloshi
 */
class PropertyFileAggregator {

    /**
     * Create a BuildProperties Object aggregated from build properties
     *
     * @param filePathsList list of all files paths to aggregate
     */
    static def aggregateBuildProperties(def filePathsList) {
        def aggregatedProperties = new Properties()
        filePathsList.each { filePath ->
            def fileProperties = extractPropertiesFromFile(filePath)
            mergeProperties(aggregatedProperties, fileProperties)
        }
        new BuildProperties(aggregatedProperties)
    }

    static def toFile(def buildProperties, def filePath){
        buildProperties.toFile(filePath)
    }

    static def mergeProperties(Properties globalProperties, Properties propertiesToMerge) {
        globalProperties.putAll(propertiesToMerge)
    }

    static def extractPropertiesFromFile(def filePath) {
        Properties properties = new Properties()
        new File(filePath).withInputStream { stream ->
            properties.load(stream)
        }
        properties
    }

}
