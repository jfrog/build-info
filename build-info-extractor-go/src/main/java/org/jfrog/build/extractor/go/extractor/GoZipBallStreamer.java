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
 * @author BarakH
 */
public class GoZipBallStreamer implements Closeable {

    protected ArchiveOutputStream archiveOutputStream;
    private final Log log;
    private ZipFile zipFile;
    private String projectName;
    private String version;
    private Set<String> excludedDirectories;
    private String subModuleName;
    private static final String MOD_FILE = "/go.mod";
    private static final String VENDOR = "vendor/";

    public GoZipBallStreamer(ZipFile zipFile, String projectName, String version, Log log) {
        this.zipFile = zipFile;
        this.projectName = projectName;
        this.version = version;
        this.log = log;
        excludedDirectories = Sets.newHashSet();
    }

    public void writeDeployableZip(File deployableZip) throws IOException {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(deployableZip)) {
            archiveOutputStream = zos;
            packProject();
            archiveOutputStream.finish();
            archiveOutputStream.flush();
        }
    }

    protected void packProject() throws IOException {
        initiateProjectType();
        scanEntries();
        writeEntries();
    }

    /**
     * Determine if the project is a compatible module from version 2+, a sub module or an incompatible module
     */
    private void initiateProjectType() {
        boolean compatibleModuleFromV2 = GoVersionUtils.isCompatibleGoModuleNaming(projectName, version, log);
        if (compatibleModuleFromV2) {
            subModuleName = "v" + GoVersionUtils.getMajorProjectVersion(projectName, log);
            log.debug(projectName + "@" + version + " is compatible Go module from major version " + subModuleName);
        } else {
            subModuleName = GoVersionUtils.getSubModule(projectName);
            if (shouldPackSubModule()) {
                log.debug(projectName + "@" + version + " is a sub module - the sub module name is " + subModuleName);
            } else {
                log.debug(projectName + "@" + version + " is a regular module");
            }
        }
    }

    /**
     * Writing all the needed entries with the correct naming convention into the output zip file
     */
    private void writeEntries() throws IOException {
        Enumeration<? extends ZipArchiveEntry> entries = zipFile.getEntries();
        ZipArchiveEntry zipEntry;
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory() && zipFile.getUnixSymlink(zipEntry) == null) {
                try {
                    if (!excludeEntry(zipEntry.getName())) {
                        ZipArchiveEntry correctedEntry = getCorrectedEntryName(zipEntry.getName(), zipEntry.getSize());
                        writeEntry(zipEntry, correctedEntry);
                        //log.trace("Entry: " + zipEntry.getName() + " - was included and renamed to " + correctedEntry);
                    } else {
                        //log.trace("Entry: " + zipEntry.getName() + " - was excluded");
                    }
                } catch (IOException e) {
                    log.error("Could not read or write entity from zip for Go package " + projectName, e);
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
        String subPath = stripFirstPathElement(entryName);
        if (shouldPackSubModule() && subPath.startsWith(subModuleName + "/")) {
            subPath = subPath.replace(subModuleName + '/', "");
        }
        String correctedEntryName = String.join("/", projectName + "@" + version, subPath);
        ZipArchiveEntry zipArchiveEntry = new ZipArchiveEntry(correctedEntryName);
        zipArchiveEntry.setSize(entryLength);
        return zipArchiveEntry;
    }

    /**
     * Following the go client implementation:
     * https://github.com/golang/go/blob/4be6b4a73d2f95752b69f5b6f6bfb4c1a7a57212/src/cmd/go/internal/modfetch/coderepo.go
     * 1. Excluding specific files, vendored packages and submodules (submodule = Each directory with go.mod file, except the one that we are packing)
     * 2. If there is a LICENSE file in the root directory, it should be included in the submodule output .zip file
     *
     * @return True if we should exclude this file from the output .zip file
     */
    private boolean excludeEntry(String entryName) {
        if (entryName.endsWith("/.hg_archival.txt")) {
            return true;
        }
        if (isVendorPackage(entryName)) {
            return true;
        }
        if (entryName.lastIndexOf('/') != -1) {
            String trimmedPrefix = entryName.substring(0, entryName.lastIndexOf('/'));
            if (shouldPackSubModule()) {
                String rootPath = trimmedPrefix.split('/' + subModuleName, 2)[0];
                excludedDirectories.remove(rootPath);
                if (rootPath.equals(trimmedPrefix)) {
                    return !entryName.endsWith("LICENSE");
                }
            }
            return (excludedDirectories.stream().parallel().anyMatch(trimmedPrefix::startsWith));
        }
        return false;
    }

    /**
     * Scanning all the original zip entries and collecting all the relative paths with go.mod files (i.e submodules)
     */
    private void scanEntries() {
        Enumeration<? extends ZipEntry> entries = zipFile.getEntries();
        ZipEntry zipEntry;
        while (entries.hasMoreElements()) {
            zipEntry = entries.nextElement();
            if (!zipEntry.isDirectory() && isSubModule(zipEntry.getName())) {
                String subModulePath = zipEntry.getName().replace(MOD_FILE, "");
                excludedDirectories.add(subModulePath);
            }
        }
    }

    /**
     * @param entryName
     * @return True if the entry is a go.mod file which doesn't belong to the project we are packing
     */
    private boolean isSubModule(String entryName) {
        if (entryName.endsWith(MOD_FILE)) {
            if (shouldPackSubModule()) {
                return (!entryName.substring(entryName.indexOf('/') + 1).contains(subModuleName + MOD_FILE));
            } else {
                return entryName.substring(entryName.indexOf('/') + 1).contains(MOD_FILE);
            }
        }
        return false;
    }

    /**
     * Based on the original go client behaviour:
     * https://github.com/golang/go/blob/4be6b4a73d2f95752b69f5b6f6bfb4c1a7a57212/src/cmd/go/internal/modfetch/coderepo.go
     *
     * @param entryName
     * @return True if the entry belongs to a vendor package
     */
    private boolean isVendorPackage(String entryName) {
        if (entryName.startsWith(VENDOR) || entryName.contains('/' + VENDOR)) {
            return entryName.substring(entryName.indexOf(VENDOR) + VENDOR.length()).contains("/");
        } else {
            return false;
        }
    }

    /**
     * @return True - in order to pack a submodule in the original .zip file
     * False - in order to pack the root project in the .zip file
     */
    private boolean shouldPackSubModule() {
        return subModuleName != null;
    }

    /**
     * @param path
     * @return First path element
     */
    private static String stripFirstPathElement(String path) {
        if (path == null) {
            return null;
        } else {
            int indexOfFirstSlash = path.indexOf('/');
            return indexOfFirstSlash < 0 ? "" : path.substring(indexOfFirstSlash + 1);
        }
    }

    @Override
    public void close() throws IOException {
        zipFile.close();
    }
}