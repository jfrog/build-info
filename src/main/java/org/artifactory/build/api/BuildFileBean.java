package org.artifactory.build.api;

/**
 * Main interface of the build file bean
 *
 * @author Noam Y. Tenne
 */
public interface BuildFileBean extends BuildBean {

    /**
     * Returns the SHA1 checksum of the file
     *
     * @return File SHA1 checksum
     */
    String getSha1();

    /**
     * Sets the SHA1 checksum of the file
     *
     * @param sha1 File SHA1 checksum
     */
    void setSha1(String sha1);

    /**
     * Returns the MD5 checksum of the file
     *
     * @return File MD5 checksum
     */
    String getMd5();

    /**
     * Sets the MD5 checksum of the file
     *
     * @param md5 File MD5 checksum
     */
    void setMd5(String md5);

    /**
     * Returns the type of the file
     *
     * @return File type
     */
    String getType();

    /**
     * Sets the type of the file
     *
     * @param type File type
     */
    void setType(String type);
}
