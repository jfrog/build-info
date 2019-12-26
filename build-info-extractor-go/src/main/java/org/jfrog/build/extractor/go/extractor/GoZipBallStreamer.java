/*
 *
 * Copyright 2018 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.jfrog.build.extractor.go.extractor;

import org.jfrog.build.api.util.Log;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.Closeable;
import java.io.IOException;
import java.io.File;
import java.util.Enumeration;
import java.util.Set;
import java.util.zip.ZipEntry;


/**
 * @author Liz Dashevski
 */
public class GoZipBallStreamer implements Closeable {

    private final Log log;

    private ArchiveOutputStream archiveOutputStream;
    private ZipFile zipFile;
    private String projectName;
    private String version;
    private Set<String> excludedDirectories;

    public GoZipBallStreamer(ZipFile zipFile, String projectName, String version, Log log) {
        this.zipFile = zipFile;
        this.projectName = projectName;
        this.version = version;
        this.log = log;
    }

    public void writeDeployableZip(File deployableZip) throws IOException {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(deployableZip)) {
            archiveOutputStream = zos;
            writeEntries();
            archiveOutputStream.finish();
            archiveOutputStream.flush();
        }
    }

    private void writeEntries() throws IOException {
        scanEntries();
        Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
        ZipArchiveEntry zipEntry;
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory() && zipFile.getUnixSymlink(zipEntry) == null) {
                try {
                    ZipArchiveEntry correctedEntry = getCorrectedEntryName(zipEntry.getName(), zipEntry.getSize());
                    if (!excludeEntry(correctedEntry.getName())) {
                        writeEntry(zipEntry, correctedEntry);
                    }
                } catch (IOException e) {
                    log.error("Could not read entity from zip for Go package " + projectName, e);
                }
            }
        }
    }

    private void writeEntry(ZipArchiveEntry originalEntry, ZipArchiveEntry correctedEntry) throws IOException {
        try {
            archiveOutputStream.putArchiveEntry(correctedEntry);
            IOUtils.copy(zipFile.getInputStream(originalEntry), archiveOutputStream);
        } finally {
            archiveOutputStream.closeArchiveEntry();
        }
    }

    /**
     * The structure of the zip is modified to be projectName/@v{version}/{path},
     *
     * @param entryName Original entry name
     * @return ArchiveEntry with modified path to be projectName/@v{version}/{path}
     */
    private ZipArchiveEntry getCorrectedEntryName(String entryName, long entryLength) {
        String subPath = entryName.substring(entryName.indexOf('/') + 1);
        String correctedEntryName = Joiner.on("/").join(projectName + "@" + version, subPath);
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(correctedEntryName);
        zipArchiveEntry.setSize(entryLength);
        return zipArchiveEntry;
    }

    private boolean excludeEntry(String entryName) {
        if (entryName.endsWith("/.hg_archival.txt")) {
            return true;
        }

        if (isVendoredPackage(entryName)) {
            return true;
        }

        if (entryName.lastIndexOf("/") != -1) {
            String trimmedPrefix = entryName.substring(0, entryName.lastIndexOf("/"));
            return excludedDirectories.contains(trimmedPrefix);
        }

        return false;
    }

    private void scanEntries() {
        excludedDirectories = Sets.newHashSet();
        Enumeration<? extends ZipEntry> entries = zipFile.getEntries();
        ZipEntry zipEntry;

        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory()) {
                if (isSubModule(zipEntry.getName())) {
                    String subModulePath = getCorrectedEntryName(zipEntry.getName(), zipEntry.getSize())
                            .getName().replace("/go.mod", "");
                    excludedDirectories.add(subModulePath);
                }
            }
        }
    }

    private boolean isSubModule(String entryName) {
        return entryName.endsWith("/go.mod") && entryName.substring(entryName.indexOf('/') + 1).contains("/");
    }

    private boolean isVendoredPackage(String entryName) {
        int i;
        if (entryName.startsWith("vendor/")) {
            i = ("vendor/").length();
        } else if (entryName.contains("/vendor/")) {
            i = ("/vendor/").length();
        } else {
            return false;
        }
        return entryName.substring(i).contains("/");
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }
}
