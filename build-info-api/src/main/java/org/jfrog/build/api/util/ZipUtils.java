package org.jfrog.build.api.util;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A utility class to perform different archive related actions
 */
public abstract class ZipUtils {

    /**
     * Extracts the given archive file into the given directory
     *
     * @param sourceArchive        Archive to extract
     * @param destinationDirectory Directory to extract achive to
     * @throws IllegalArgumentException Thrown when given invalid destinations
     * @throws IOException              Thrown when any error occures while extracting
     */
    public static void extract(File sourceArchive, File destinationDirectory) throws IOException {
        if ((sourceArchive == null) || (destinationDirectory == null)) {
            throw new IllegalArgumentException("Supplied destinations cannot be null.");
        }
        if (!sourceArchive.isFile()) {
            throw new IllegalArgumentException("Supplied source archive must be an existing file.");
        }
        extractFiles(sourceArchive, destinationDirectory.getCanonicalFile());
    }

    /**
     * Extracts the given archive file into the given directory
     *
     * @param sourceArchive        Archive to extract
     * @param destinationDirectory Directory to extract archive to
     */
    private static void extractFiles(File sourceArchive, File destinationDirectory) {
        ArchiveInputStream archiveInputStream = null;
        try {
            archiveInputStream = createArchiveInputStream(sourceArchive);
            extractFiles(archiveInputStream, destinationDirectory);
        } catch (IOException ioe) {
            throw new RuntimeException("Error while extracting " + sourceArchive.getPath(), ioe);
        } finally {
            IOUtils.closeQuietly(archiveInputStream);
        }
    }

    private static void extractFiles(ArchiveInputStream archiveInputStream, File destinationDirectory) throws IOException {
        ArchiveEntry entry;
        while ((entry = archiveInputStream.getNextEntry()) != null) {
            //Validate entry name before extracting
            String validatedEntryName = validateEntryName(entry.getName());

            if (StringUtils.isNotBlank(validatedEntryName)) {
                // ZipArchiveEntry does not carry relevant info
                // for symlink identification, thus it not supported
                // at this stage
                extractFile(destinationDirectory, archiveInputStream, validatedEntryName,
                        entry.getLastModifiedDate(), entry.isDirectory());
            }
        }
    }

    /**
     * Get archive input stream from File Object
     *
     * @param sourceArchive - archive File
     * @return archive input stream
     * @throws IOException
     */
    private static ArchiveInputStream createArchiveInputStream(File sourceArchive) throws IOException {
        String fileName = sourceArchive.getName();
        String extension = PathUtils.getExtension(fileName);
        verifySupportedExtension(extension);
        FileInputStream fis = new FileInputStream(sourceArchive);
        ArchiveInputStream archiveInputStream = returnArchiveInputStream(fis, extension);
        if (archiveInputStream != null) {
            return archiveInputStream;
        }
        throw new IllegalArgumentException("Unsupported archive extension: '" + extension + "'");
    }

    /**
     * Return archive input stream
     *
     * @param inputStream   - file  input Stream
     * @param archiveSuffix - archive suffix
     * @return archive input stream
     * @throws IOException
     */
    private static ArchiveInputStream returnArchiveInputStream(InputStream inputStream, String archiveSuffix)
            throws IOException {
        if (isZipFamilyArchive(archiveSuffix)) {
            return new ZipArchiveInputStream(inputStream);
        }

        if (isTarArchive(archiveSuffix)) {
            return new TarArchiveInputStream(inputStream);
        }

        if (isTgzFamilyArchive(archiveSuffix) || isGzCompress(archiveSuffix)) {
            return new TarArchiveInputStream(new GzipCompressorInputStream(inputStream));
        }
        return new ZipArchiveInputStream(inputStream);
    }

    /**
     * Is file suffix related to gz compress
     *
     * @param archiveSuffix - archive file suffix
     * @return
     */
    private static boolean isGzCompress(String archiveSuffix) {
        return archiveSuffix.equals("gz");
    }

    /**
     * Is file suffix related to tar archive
     *
     * @param archiveSuffix - archive suffix
     * @return
     */
    private static boolean isTarArchive(String archiveSuffix) {
        return archiveSuffix.endsWith("tar");
    }

    private static boolean isTgzFamilyArchive(String archiveSuffix) {
        return archiveSuffix.endsWith("tar.gz") || archiveSuffix.endsWith("tgz");
    }

    private static boolean isZipFamilyArchive(String archiveSuffix) {
        return archiveSuffix.endsWith("zip") || archiveSuffix.endsWith("jar") || archiveSuffix.toLowerCase().endsWith(
                "nupkg") || archiveSuffix.endsWith("war");
    }

    private static void verifySupportedExtension(String extension) {
        Set<String> supportedExtensions = Sets.newHashSet();
        try {
            String supportedExtensionsNames = "zip,tar,tar.gz,tgz";
            supportedExtensions = Sets.newHashSet(
                    Iterables.transform(Sets.newHashSet(StringUtils.split(supportedExtensionsNames, ",")),
                            new Function<String, String>() {
                                @Override
                                public String apply(String input) {
                                    String result = StringUtils.isBlank(input) ? input : StringUtils.trim(input);
                                    return StringUtils.equals(result, "tar.gz") ? "gz" : result;
                                }
                            }
                    )
            );
        } catch (Exception e) {
        }

        if (StringUtils.isBlank(extension) || !supportedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported archive extension: '" + extension + "'");
        }
    }

    /**
     * Extracts the given zip entry
     *
     * @param sourcePath           Path of archive that is being extracted
     * @param destinationDirectory Extracted file destination
     * @param zipInputStream       Input stream of archive
     * @param entryName            Entry to extract
     * @param entryDate            Last modification date of zip entry
     * @param isEntryDirectory     Indication if the entry is a directory or not
     * @throws IOException
     */
    private static void extractFile(File destinationDirectory, InputStream zipInputStream,
                                    String entryName, Date entryDate, boolean isEntryDirectory) throws IOException {

        File resolvedEntryFile = new File(destinationDirectory, entryName);
        try {
            File parentFile = resolvedEntryFile.getParentFile();

            //If the parent file isn't null, attempt to create it because it might not exist
            if (parentFile != null) {
                parentFile.mkdirs();
            }

            if (isEntryDirectory) {
                //Create directory entry
                resolvedEntryFile.mkdirs();
            } else {
                //Extract file entry
                byte[] buffer = new byte[1024];
                int length;
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(resolvedEntryFile);

                    while ((length = zipInputStream.read(buffer)) >= 0) {
                        fileOutputStream.write(buffer, 0, length);
                    }
                } finally {
                    IOUtils.closeQuietly(fileOutputStream);
                }
            }

            //Preserve last modified date
            resolvedEntryFile.setLastModified(entryDate.getTime());
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Can't extract file. ", ex);
        }
    }

    /**
     * Validates the given entry name by removing different slashes that might appear in the begining of the name and
     * any occurences of relative paths like "../", so we can protect from path traversal attacks
     *
     * @param entryName Name of zip entry
     */
    private static String validateEntryName(String entryName) {
        entryName = FilenameUtils.separatorsToUnix(entryName);
        entryName = PathUtils.trimLeadingSlashes(entryName);
        entryName = removeDotSegments(entryName);

        return entryName;
    }

    //"Borrowed" from com.sun.jersey.server.impl.uri.UriHelper
    // alg taken from http://gbiv.com/protocols/uri/rfc/rfc3986.html#relative-dot-segments
    // the alg works as follows:
    //       1. The input buffer is initialized with the now-appended path components and the output buffer is initialized to the empty string.
    //   2. While the input buffer is not empty, loop as follows:
    //         A. If the input buffer begins with a prefix of "../" or "./", then remove that prefix from the input buffer; otherwise,
    //         B. if the input buffer begins with a prefix of "/./"
    //            or "/.", where "." is a complete path segment, then replace that prefix with "/" in the input buffer; otherwise,
    //         C. if the input buffer begins with a prefix of "/../"
    //            or "/..", where ".." is a complete path segment,
    //            then replace that prefix with "/" in the input buffer and remove the last segment and its preceding "/" (if any) from the output buffer; otherwise,
    //         D. if the input buffer consists only of "." or "..", then remove that from the input buffer; otherwise,
    //         E. move the first path segment in the input buffer to the end of the output buffer,
    //            including the initial "/" character (if any) and any subsequent characters up to, but not including,
    //            the next "/" character or the end of the input buffer.
    //   3. Finally, the output buffer is returned as the result of remove_dot_segments.

    @SuppressWarnings({"OverlyComplexMethod"})
    private static String removeDotSegments(String path) {

        if (null == path) {
            return null;
        }

        List<String> outputSegments = new LinkedList<String>();

        while (path.length() > 0) {
            if (path.startsWith("../")) {   // rule 2A
                path = PathUtils.trimLeadingSlashes(path.substring(3));
            } else if (path.startsWith("./")) { // rule 2A
                path = PathUtils.trimLeadingSlashes(path.substring(2));
            } else if (path.startsWith("/./")) { // rule 2B
                path = "/" + PathUtils.trimLeadingSlashes(path.substring(3));
            } else if ("/.".equals(path)) { // rule 2B
                path = "/";
            } else if (path.startsWith("/../")) { // rule 2C
                path = "/" + PathUtils.trimLeadingSlashes(path.substring(4));
                if (!outputSegments.isEmpty()) { // removing last segment if any
                    outputSegments.remove(outputSegments.size() - 1);
                }
            } else if ("/..".equals(path)) { // rule 2C
                path = "/";
                if (!outputSegments.isEmpty()) { // removing last segment if any
                    outputSegments.remove(outputSegments.size() - 1);
                }
            } else if ("..".equals(path) || ".".equals(path)) { // rule 2D
                path = "";
            } else { // rule E
                int slashStartSearchIndex;
                if (path.startsWith("/")) {
                    path = "/" + PathUtils.trimLeadingSlashes(path.substring(1));
                    slashStartSearchIndex = 1;
                } else {
                    slashStartSearchIndex = 0;
                }
                int segLength = path.indexOf('/', slashStartSearchIndex);
                if (-1 == segLength) {
                    segLength = path.length();
                }
                outputSegments.add(path.substring(0, segLength));
                path = path.substring(segLength);
            }
        }

        StringBuffer result = new StringBuffer();
        for (String segment : outputSegments) {
            result.append(segment);
        }

        return result.toString();
    }

    private static class PathUtils {

        /**
         * @param path The path (usually of a file)
         * @return The file extension. Null if file name has no extension. For example 'file.xml' will return xml, 'file'
         * will return null.
         */
        public static String getExtension(String path) {
            if (path == null) {
                return null;
            }
            // TODO: check there is no slash after this dot
            int dotPos = path.lastIndexOf('.');
            if (dotPos < 0) {
                return null;
            }
            return path.substring(dotPos + 1);
        }

        public static String trimLeadingSlashes(CharSequence path) {
            CharSequence res = trimLeadingSlashChars(path);
            return res != null ? res.toString() : null;
        }

        public static CharSequence trimLeadingSlashChars(CharSequence path) {
            if (path == null) {
                return null;
            }
            //Trim leading '/' (caused by webdav requests)
            if (path.length() > 0 && path.charAt(0) == '/') {
                path = path.subSequence(1, path.length());
                return trimLeadingSlashChars(path);
            }
            return path;
        }

        public static String trimTrailingSlashes(CharSequence path) {
            CharSequence res = trimTrailingSlashesChars(path);
            return res != null ? res.toString() : null;
        }

        public static CharSequence trimTrailingSlashesChars(CharSequence path) {
            if (path == null) {
                return null;
            }
            if (path.length() > 0 && path.charAt(path.length() - 1) == '/') {
                path = path.subSequence(0, path.length() - 1);
                return trimTrailingSlashes(path);
            }
            return path;
        }
    }
}
