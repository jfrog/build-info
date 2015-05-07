package org.jfrog.build.utils

/**
 * @author Aviad Shikloshi
 */
class BuildProperties {

    private def properties

    BuildProperties(){
        properties = new Properties()
    }

    BuildProperties(Properties propertiesMap){
        this.properties = propertiesMap
    }

    def toFile(def filePath){
        properties.store(new FileOutputStream(filePath), "")
    }

    def getBuildProperty(def key){
        if (!properties.containsKey(key)){
            throw new IllegalArgumentException("${key} do not exists in the build configuration.")
        }
        properties.get(key)
    }

}
