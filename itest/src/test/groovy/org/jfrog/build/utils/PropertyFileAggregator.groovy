package org.jfrog.build.utils

import org.apache.commons.io.IOUtils

/**
 * @author Aviad Shikloshi
 */
class PropertyFileAggregator {

    /**
     * Create a BuildProperties Object aggregated from build properties
     *
     * @param filePathsList list of all files paths to aggregate
     */
    static def aggregateBuildProperties(ConfigObject config) {
        def aggregatedProperties = new Properties()
        def filePath = this.getClass().getResource("/org/jfrog/build/defaultBuildInfo.properties").path
        aggregatedProperties.load(new FileInputStream(filePath))
        Map flat = config.getProperty("buildInfoProperties").flatten()
        aggregatedProperties.putAll(flat)

        aggregatedProperties
    }

    static def toFile(Properties buildProperties){
        File tempFile = File.createTempFile("buildInfo", ".properties");
        FileOutputStream stream = new FileOutputStream(tempFile);
        try {
            buildProperties.store(stream, "");
        } finally {
            IOUtils.closeQuietly(stream);
        }

        tempFile.path
    }
}
