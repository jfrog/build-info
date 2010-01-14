package org.artifactory.build.api;

/**
 * Build service main implementation
 *
 * @author Noam Y. Tenne
 */
public interface BuildService {

    /**
     * Adds the given build to the DB
     *
     * @param build Build to add
     */
    void addBuild(Build build);

    /**
     * Converts the given build to XML
     *
     * @param build Build to convert
     * @return Build XML
     */
    String getXmlFromBuild(Build build);

    /**
     * Converts the given XML to a build object
     *
     * @param buildXml XML to convert
     * @return Build object
     */
    Build getBuildFromXml(String buildXml);

    /**
     * Removes the given build
     *
     * @param build Build to remove
     */
    void deleteBuild(Build build);
}