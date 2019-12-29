/*
 * Copyright 2018 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * The code in this file was taken from https://github.com/golang/go,
 * which is under this license https://github.com/golang/go/blob/master/LICENSE
 * Copyright (c) 2009 The Go Authors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following disclaimer
 *    in the documentation and/or other materials provided with the
 *    distribution.
 *  * Neither the name of Google Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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
